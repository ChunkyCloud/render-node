/*
 * rs-rendernode is the worker node software of our RenderService.
 * Copyright (C) 2016 Wertarbyte <https://wertarbyte.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.lemaik.renderservice.renderer.rendering;

import com.google.gson.Gson;
import de.lemaik.renderservice.renderer.chunky.ChunkyWrapper;
import de.lemaik.renderservice.renderer.chunky.RenderException;
import de.lemaik.renderservice.renderer.util.FileUtil;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskWorker.class);

    private final Path workingDir;
    private final Path texturepacksDir;
    private final ChunkyWrapper chunky;
    private final RenderServerApiClient apiClient;

    public TaskWorker(Path workingDir, Path texturepacksDir, int threads, int cpuLoad, RenderServerApiClient apiClient) {
        this.workingDir = workingDir;
        this.texturepacksDir = texturepacksDir;
        this.chunky = new ChunkyWrapper(threads, cpuLoad);
        chunky.setDefaultTexturepack(texturepacksDir.toFile());
        this.apiClient = apiClient;
    }

    public void loadScene(Task task) throws ExecutionException, InterruptedException, TimeoutException, IOException {
        LOGGER.info("Loading scene for task {} for job {}", task.getId(), task.getJob().getId());

        LOGGER.info("Downloading scene files...");
        final File skymap = null; /*job.getSkymapUrl().isPresent()
          ? apiClient.downloadSkymapTo(job.getSkymapUrl().get(), workingDir).get().getAbsoluteFile()
          : null;*/ //TODO skymap

        CompletableFuture.allOf(
                apiClient.getScene(task).thenAccept((scene -> {
                    scene.addProperty("name", "scene");
                    if (skymap != null) {
                        scene.getAsJsonObject("sky").addProperty("skymap", skymap.getAbsolutePath());
                    }
                    try (OutputStreamWriter out = new OutputStreamWriter(
                            new FileOutputStream(new File(workingDir.toFile(), "scene.json")))) {
                        new Gson().toJson(scene, out);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })),
                apiClient.downloadOctree(task, new File(workingDir.toFile(), "scene.octree2")),
                apiClient.downloadEmittergrid(task, new File(workingDir.toFile(), "scene.emittergrid"))
        ).get(4, TimeUnit.HOURS); // timeout after 4 hours of downloading

        File texturepack = null; // TODO resourcepacks
            /*
            if (job.getTexturepack() != null) {
                texturepack = new File(texturepacksDir.toFile(), job.getTexturepack() + ".zip");
                if (!texturepack.isFile()) {
                    LOGGER.info("Downloading texturepack...");
                    apiClient.downloadResourcepack(job.getTexturepack(), texturepack).get(4, TimeUnit.HOURS);
                }
            }
             */

        chunky.loadScene(texturepack, new File(workingDir.toFile(), "scene.json"));
    }

    public void renderScene(Task task) throws RenderException {
        LOGGER.info("Render scene for task {} for job {}", task.getId(), task.getJob().getId());

        ScheduledExecutorService progressReportScheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            Future<ChunkyWrapper.RenderResult> renderFuture;
            try {
                LOGGER.info("Rendering...");
                renderFuture = chunky.render(task);
            } catch (InterruptedException e) {
                throw new RenderException("Rendering interrupted", e);
            }

            AtomicBoolean rendering = new AtomicBoolean(true);

            progressReportScheduler.scheduleAtFixedRate(() -> {
                try {
                    RenderServerApiClient.ProgressReportResult result = apiClient.reportTaskProgress(task.getId(), chunky.getCurrentSpp()).get(3, TimeUnit.SECONDS);
                    if (result == RenderServerApiClient.ProgressReportResult.STOP_RENDERING && rendering.get()) {
                        LOGGER.info("Render task {} has been aborted, interrupting renderer", task.getId());
                        renderFuture.cancel(true);
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    LOGGER.error("Failed to report task progress", e);
                }
            }, 0, 5, TimeUnit.SECONDS);

            ChunkyWrapper.RenderResult result;
            try {
                result = renderFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RenderException("Rendering failed", e);
            }
            rendering.set(false);

            LOGGER.info("Uploading...");
            try {
                FinishTaskRenderingResponse.UploadUrls uploadUrls = apiClient.finishTaskRendering(task.getId()).get().getUploadUrls();
                if (uploadUrls.getDump() != null) {
                    try (Buffer dumpBuffer = new Buffer()) {
                        result.writeDump(dumpBuffer.outputStream());
                        apiClient.uploadFile(uploadUrls.getDump(), dumpBuffer, "application/octet-stream").get();
                    }
                }
                try (Buffer imageBuffer = new Buffer()) {
                    result.writePngImage(imageBuffer.outputStream());
                    apiClient.uploadFile(uploadUrls.getImage(), imageBuffer, "image/png").get();
                }
                apiClient.finishTask(task.getId()).get();
            } catch (InterruptedException | ExecutionException | IOException e) {
                throw new RenderException("Upload failed", e);
            }
            LOGGER.info("Done");
        } finally {
            progressReportScheduler.shutdownNow();
            FileUtil.deleteDirectory(workingDir.toFile());
        }
    }
}
