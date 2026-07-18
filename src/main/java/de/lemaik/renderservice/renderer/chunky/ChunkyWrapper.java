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
import se.llbit.chunky.resources.ResourcePackLoader;
import se.llbit.util.ProgressListener;
import se.llbit.util.TaskTracker;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ChunkyWrapper {
    private final VoidRenderContext context;
    private final SynchronousSceneManager sceneManager;
    private final DefaultRenderManager renderer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private File defaultTexturepack;
    private File previousTexturepack;

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

    public void loadScene(File texturepack, File scene) throws IOException {
        if (texturepack == null) {
            texturepack = defaultTexturepack;
        }

        // all chunky instances share their texturepacks statically
        if (!texturepack.equals(previousTexturepack)) {
            if (texturepack.equals(defaultTexturepack)) {
                ResourcePackLoader.loadResourcePacks(Collections.singletonList(defaultTexturepack));
            } else {
                // load the selected texturepack and the default texturepack as fallback
                ResourcePackLoader.loadResourcePacks(Arrays.asList(texturepack, defaultTexturepack));
            }
            previousTexturepack = texturepack;
        }

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
                scene.setPostprocess(PostProcessingFilters.NONE);
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

    public void setDefaultTexturepack(File texturepackPath) {
        this.defaultTexturepack = texturepackPath;
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
