package de.lemaik.renderservice.renderer.chunky;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;

import se.llbit.chunky.renderer.*;
import se.llbit.chunky.renderer.postprocessing.PostProcessingFilters;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.renderer.scene.SynchronousSceneManager;
import se.llbit.chunky.resources.TexturePackLoader;
import se.llbit.log.Log;
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
    context.setSppPerPass(targetSpp); // render in a single pass for minimal overhead
    RenderManager renderer = new DefaultRenderManager(context, true);
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

    try {
      sceneManager.getScene().setPostprocess(PostProcessingFilters.NONE);
      sceneManager.getScene().setTargetSpp(targetSpp);
      sceneManager.getScene().startHeadlessRender();
      renderer.start();
      renderer.join();
      renderer.shutdown();

      RenderStatus status = renderer.getRenderStatus();
      Scene renderedScene = sceneManager.getScene();
      renderedScene.renderTime = status.getRenderTime();
      renderedScene.spp = status.getSpp();
      saveDumpClassic(context, renderedScene);
      result.complete(context.getDump());
    } catch (InterruptedException e) {
      result.completeExceptionally(new RenderException("Rendering failed", e));
    }

    return result;
  }

  private static void saveDumpClassic(RenderContext context, Scene scene) {
    String fileName = scene.name + ".dump";
    double[] samples = scene.getSampleBuffer();
    try (DataOutputStream out =
                 new DataOutputStream(new GZIPOutputStream(context.getSceneFileOutputStream(fileName)))) {
      out.writeInt(scene.width);
      out.writeInt(scene.height);
      out.writeInt(scene.spp);
      out.writeLong(scene.renderTime);
      for (int x = 0 ; x < scene.width; x++) {
        for (int y = 0; y < scene.height; y++) {
          int offset = (y * scene.width + x) * 3;
          for (int i = 0; i < 3; i++) {
            out.writeDouble(samples[offset + i]);
          }
        }
      }
    } catch (IOException e) {
      Log.warn("IO Exception while saving render dump!", e);
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
}
