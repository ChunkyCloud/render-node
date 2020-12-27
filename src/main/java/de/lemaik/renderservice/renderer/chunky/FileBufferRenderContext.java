package de.lemaik.renderservice.renderer.chunky;


import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.main.ChunkyOptions;
import se.llbit.chunky.renderer.RenderContext;

/**
 * A mocked {@link RenderContext} for Chunky that saves scene files into buffers. Only supports
 * saving scenes.
 */
public class FileBufferRenderContext extends RenderContext {

  private ByteArrayOutputStream scene;
  private ByteArrayOutputStream grass;
  private ByteArrayOutputStream foliage;
  private ByteArrayOutputStream octree;
  private ByteArrayOutputStream dump;

  public FileBufferRenderContext() {
    super(new Chunky(ChunkyOptions.getDefaults()));
  }

  @Override
  public OutputStream getSceneFileOutputStream(String fileName) throws FileNotFoundException {
    if (fileName.endsWith(".json")) {
      return scene = new ByteArrayOutputStream();
    } else if (fileName.endsWith(".grass")) {
      return grass = new ByteArrayOutputStream();
    } else if (fileName.endsWith(".foliage")) {
      return foliage = new ByteArrayOutputStream();
    } else if (fileName.endsWith(".octree")) {
      return octree = new ByteArrayOutputStream();
    } else if (fileName.endsWith(".dump")) {
      return dump = new ByteArrayOutputStream();
    }

    return new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        // no-op
      }
    };
  }

  public byte[] getScene() {
    return scene.toByteArray();
  }

  public byte[] getGrass() {
    return grass.toByteArray();
  }

  public byte[] getFoliage() {
    return foliage.toByteArray();
  }

  public byte[] getOctree() {
    return octree.toByteArray();
  }

  public void setRenderThreadCount(int threads) {
    config.renderThreads = threads;
  }

  public void setSppPerPass(int sppPerPass) {
    config.sppPerPass = sppPerPass;
  }

  public byte[] getDump() {
    return dump.toByteArray();
  }
}
