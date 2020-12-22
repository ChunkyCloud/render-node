package de.lemaik.renderservice.renderer.chunky;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import se.llbit.chunky.renderer.RenderManager;
import se.llbit.chunky.renderer.RenderStatus;
import se.llbit.chunky.renderer.SnapshotControl;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.renderer.scene.SynchronousSceneManager;
import se.llbit.chunky.resources.TexturePackLoader;
import se.llbit.util.ProgressListener;
import se.llbit.util.TaskTracker;

public class EmbeddedChunkyWrapper implements ChunkyWrapper {

  private final FileBufferRenderContext context = new FileBufferRenderContext();
  private File defaultTexturepack;
  private File previousTexturepack;

  @Override
  public CompletableFuture<byte[]> render(File texturepack, File scene, int targetSpp, int threads,
      int cpuLoad)
      throws IOException, InterruptedException {
    if (texturepack == null) {
      texturepack = defaultTexturepack;
    }

    // all chunky instances share their texturepacks statically
    if (!texturepack.equals(previousTexturepack)) {
      if (texturepack.equals(defaultTexturepack)) {
        TexturePackLoader
            .loadTexturePacks(new String[]{defaultTexturepack.getAbsolutePath()}, false);
      } else {
        // load the selected texturepack and the default texturepack as fallback
        TexturePackLoader.loadTexturePacks(
            new String[]{texturepack.getAbsolutePath(), defaultTexturepack.getAbsolutePath()},
            false);
      }
      previousTexturepack = texturepack;
    }

    CompletableFuture<byte[]> result = new CompletableFuture<>();

    context.setRenderThreadCount(threads);
    RenderManager renderer = new RenderManager(context, true);
    try {
      renderer.setCPULoad(cpuLoad);

      SynchronousSceneManager sceneManager = new SynchronousSceneManager(context, renderer);
      context.setSceneDirectory(scene.getParentFile());
      sceneManager.getScene()
          .loadScene(context,
              scene.getName().substring(0, scene.getName().length() - ".json".length()),
              new TaskTracker(ProgressListener.NONE));

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

      renderer.setOnRenderCompleted((time, sps) -> {
        RenderStatus status = renderer.getRenderStatus();
        Scene renderedScene = sceneManager.getScene();
        renderedScene.renderTime = status.getRenderTime();
        renderedScene.spp = status.getSpp();
        renderedScene.saveDump(context, new TaskTracker(ProgressListener.NONE));
        result.complete(context.getDump());
      });

      try {
        sceneManager.getScene().setTargetSpp(targetSpp);
        sceneManager.getScene().startHeadlessRender();
        renderer.start();
        renderer.join();
        renderer.shutdown();
      } catch (InterruptedException e) {
        result.completeExceptionally(new RenderException("Rendering failed", e));
      }

      return result;
    } finally {
      stopRenderer(renderer);
    }
  }

  @Override
  public void stop() {

  }

  @Override
  public void addListener(RenderListener listener) {

  }

  @Override
  public void removeListener(RenderListener listener) {

  }

  @Override
  public void setTargetSpp(int targetSpp) {

  }

  @Override
  public void setThreadCount(int threadCount) {

  }

  @Override
  public void setDefaultTexturepack(File texturepackPath) {
    this.defaultTexturepack = texturepackPath;
  }

  /**
   * Stops the given renderer. The worker threads of RenderManager are created in its constructor.
   * Without this method, they would never be removed, causing threads to leak.
   *
   * @param renderer Renderer to stop
   */
  private static void stopRenderer(RenderManager renderer) {
    try {
      Method stopWorkers = RenderManager.class.getDeclaredMethod("stopWorkers");
      stopWorkers.setAccessible(true);
      stopWorkers.invoke(renderer);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
  }
}
