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
import com.google.gson.JsonObject;
import de.lemaik.renderservice.renderer.chunky.ChunkyRenderDump;
import de.lemaik.renderservice.renderer.chunky.ChunkyWrapper;
import de.lemaik.renderservice.renderer.message.Message;
import de.lemaik.renderservice.renderer.message.TaskMessage;
import de.lemaik.renderservice.renderer.util.FileUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TaskWorker implements Runnable {

  private static final Logger LOGGER = LogManager.getLogger(TaskWorker.class);
  private static final Gson gson = new Gson();

  private final TaskMessage task;
  private final MessageClient connection;
  private final Path workingDir;
  private final Path texturepacksDir;
  private final int threads;
  private final int cpuLoad;
  private final ChunkyWrapper chunky;
  private final RenderServerApiClient apiClient;

  public TaskWorker(TaskMessage task, MessageClient connection, Path workingDir,
                    Path texturepacksDir, int threads, int cpuLoad, ChunkyWrapper chunky,
                    RenderServerApiClient apiClient) {
    this.task = task;
    this.connection = connection;
    this.workingDir = workingDir;
    this.texturepacksDir = texturepacksDir;
    this.threads = threads;
    this.cpuLoad = cpuLoad;
    this.chunky = chunky;
    this.apiClient = apiClient;
  }

  @Override
  public void run() {
    try {
      Task task = Task.fromMessage(this.task);
      LOGGER.info("New task: {} spp for job {}", task.getSpp(), task.getJobId());
      Job job = apiClient.getJob(task.getJobId()).get(10, TimeUnit.MINUTES);
      if (job == null) {
        LOGGER.info("Job was deleted, skipping the task and removing it from the queue");
        connection.send(Message.taskComplete());
        return;
      }
      if (job.isCancelled()) {
        LOGGER.info("Job is cancelled, skipping the task and removing it from the queue");
        connection.send(Message.taskComplete());
        return;
      }
      if (job.getSpp() >= job.getTargetSpp()) {
        LOGGER.info(
            "Job is effectively finished ({} of {} spp), skipping the task and removing it from the queue",
            job.getSpp(), job.getTargetSpp());
        connection.send(Message.taskComplete());
        return;
      }

      final JsonObject[] sceneDescription = new JsonObject[1];
      LOGGER.info("Downloading scene files...");
      final File skymap = job.getSkymapUrl().isPresent()
          ? apiClient.downloadSkymapTo(job.getSkymapUrl().get(), workingDir).get().getAbsoluteFile()
          : null;

      CompletableFuture.allOf(
          apiClient.getScene(job).thenAccept((scene -> {
            scene.addProperty("name", "scene");
            sceneDescription[0] = scene;
            if (skymap != null) {
              scene.getAsJsonObject("sky").addProperty("skymap", skymap.getAbsolutePath());
            }
            try (OutputStreamWriter out = new OutputStreamWriter(
                new FileOutputStream(new File(workingDir.toFile(), "scene.json")))) {
              new Gson().toJson(scene, out);
            } catch (IOException e) {
              // TODO
              e.printStackTrace();
            }
          })),
          // apiClient.downloadFoliage(job, new File(workingDir.toFile(), "scene.foliage")),
          // apiClient.downloadGrass(job, new File(workingDir.toFile(), "scene.grass")),
          apiClient.downloadOctree(job, new File(workingDir.toFile(), "scene.octree2")),
          apiClient.downloadEmittergrid(job, new File(workingDir.toFile(), "scene.emittergrid"))
      ).get(4, TimeUnit.HOURS); // timeout after 4 hours of downloading

      File texturepack = null;
      if (job.getTexturepack() != null) {
        texturepack = new File(texturepacksDir.toFile(), job.getTexturepack() + ".zip");
        if (!texturepack.isFile()) {
          LOGGER.info("Downloading texturepack...");
          apiClient.downloadResourcepack(job.getTexturepack(), texturepack).get(4, TimeUnit.HOURS);
        }
      }

      LOGGER.info("Rendering...");
      byte[] dump = chunky
          .render(texturepack, new File(workingDir.toFile(), "scene.json"), task.getSpp(),
              threads,
              cpuLoad).get();

      LOGGER.info("Uploading...");
      if (job.isPictureOnly() && job.getTargetSpp() <= task.getSpp()) {
        LOGGER.info("Dump not needed, uploading picture instead");
        ChunkyRenderDump renderDump = ChunkyRenderDump
            .fromStream(new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(dump))));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
          BufferedImage image = renderDump.getPicture(
              sceneDescription[0].get("exposure").getAsDouble(),
              ChunkyRenderDump.Postprocess
                  .valueOf(sceneDescription[0].get("postprocess").getAsString())
          );
          ImageIO.write(image, "png", bos);
          byte[] picture = bos.toByteArray();
          try (Response res = apiClient.postPicture(job.getId(), picture, renderDump.getSpp())
              .get(1, TimeUnit.HOURS)) {
            if (res.code() == 409) {
              LOGGER.info("Picture upload rejected, uploading dump instead");
              apiClient.postDump(job.getId(), dump).get(1, TimeUnit.HOURS);
            }
          }
        }
      } else {
        apiClient.postDump(job.getId(), dump).get(1, TimeUnit.HOURS);
      }

      connection.send(Message.taskComplete());
      LOGGER.info("Done");
    } catch (Exception e) {
      LOGGER.warn("An error occurred while processing a task", e);
      connection.close();
    } finally {
      FileUtil.deleteDirectory(workingDir.toFile());
    }
  }
}
