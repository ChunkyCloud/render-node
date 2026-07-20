/*
 * Copyright (C) 2020-2026 leMaik and contributors
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

package de.lemaik.renderservice.renderer.chunky;

import de.lemaik.renderservice.renderer.rendering.Task;
import se.llbit.chunky.renderer.DefaultRenderManager;
import se.llbit.chunky.renderer.RenderStatus;
import se.llbit.chunky.renderer.SnapshotControl;
import se.llbit.chunky.renderer.export.PictureExportFormats;
import se.llbit.chunky.renderer.postprocessing.PostProcessingFilters;
import se.llbit.chunky.renderer.renderdump.RenderDump;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.renderer.scene.SynchronousSceneManager;
import se.llbit.util.ProgressListener;
import se.llbit.util.TaskTracker;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ChunkyWrapper {
    private final VoidRenderContext context;
    private final SynchronousSceneManager sceneManager;
    private final DefaultRenderManager renderer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ChunkyWrapper(int threads, int cpuLoad) {
        context = new VoidRenderContext();
        context.setRenderThreadCount(threads);
        renderer = new DefaultRenderManager(context, true);
        sceneManager = new SynchronousSceneManager(context, renderer);
        renderer.setCPULoad(cpuLoad);
        renderer.setSceneProvider(sceneManager);
        renderer.setSnapshotControl(new SnapshotControl() {
            @Override
            public boolean saveSnapshot(Scene scene, int nextSpp) {
                return false;
            }

            @Override
            public boolean saveRenderDump(Scene scene, int nextSpp) {
                return false;
            }
        });
    }

    public void loadScene(File scene) throws IOException {
        context.setSceneDirectory(scene.getParentFile());
        sceneManager.getScene()
                .loadScene(context,
                        scene.getName().substring(0, scene.getName().length() - ".json".length()),
                        new TaskTracker(ProgressListener.NONE));
    }

    public Future<RenderResult> render(Task task) throws InterruptedException {
        return executor.submit(() -> {
            context.setSppPerPass(1);
            sceneManager.getScene().refresh();
            sceneManager.withEditSceneProtected(scene -> {
                task.getTile().applyToScene(scene, task.getJob().getWidth(), task.getJob().getHeight());
                scene.setTargetSpp(task.getSpp());
            });
            sceneManager.applySceneChanges();
            sceneManager.getScene().startHeadlessRender();
            renderer.run();

            RenderStatus status = renderer.getRenderStatus();
            Scene renderedScene = sceneManager.getScene();
            renderedScene.renderTime = status.getRenderTime();
            renderedScene.spp = status.getSpp();

            return new RenderResult() {
                @Override
                public void writePngImage(OutputStream outputStream) throws IOException {
                    renderer.bufferedScene.writeFrame(outputStream, PictureExportFormats.PNG, TaskTracker.NONE);
                }

                @Override
                public void writeDump(OutputStream outputStream) throws IOException {
                    RenderDump.save(outputStream, renderer.bufferedScene, TaskTracker.NONE);
                }
            };
        });
    }

    public int getCurrentSpp() {
        return renderer.getRenderStatus().getSpp();
    }

    public abstract class RenderResult {
        private RenderResult() {
        }

        public abstract void writePngImage(OutputStream outputStream) throws IOException;

        public abstract void writeDump(OutputStream outputStream) throws IOException;
    }
}
