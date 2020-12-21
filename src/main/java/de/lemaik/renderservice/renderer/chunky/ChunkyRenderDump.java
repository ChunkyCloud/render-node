/*
 * Copyright (c) 2013-2015 Wertarbyte <http://wertarbyte.com>
 *
 * This file is part of Wertarbyte RenderService.
 *
 * Wertarbyte RenderService is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wertarbyte RenderService is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wertarbyte RenderService.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.lemaik.renderservice.renderer.chunky;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A render dump.
 * <p/>
 * Code adapted from: https://github.com/llbit/chunky/blob/master/chunky/src/java/se/llbit/chunky/renderer/scene/Scene.java
 */
public class ChunkyRenderDump {

  private static final float DEFAULT_GAMMA = 2.2f;

  private final int width;
  private final int height;
  private int spp;
  private long renderTime;
  private final double[] samples;

  public ChunkyRenderDump(int width, int height) {
    this.width = width;
    this.height = height;
    samples = new double[width * height * 3];
  }

  public ChunkyRenderDump(int width, int height, double[] samples, int spp, long renderTime) {
    this.width = width;
    this.height = height;
    if (samples.length != 3 * width * height) {
      throw new IllegalArgumentException("Sample array has invalid size");
    }
    this.samples = Arrays.copyOf(samples, samples.length);
    this.spp = spp;
    this.renderTime = renderTime;
  }

  public static ChunkyRenderDump loadFile(File dumpFile) throws IOException {
    try (DataInputStream in = new DataInputStream(
        new GZIPInputStream(new FileInputStream(dumpFile)))) {
      return fromStream(in);
    }
  }

  public static ChunkyRenderDump fromStream(DataInputStream in) throws IOException {
    int width = in.readInt();
    int height = in.readInt();
    double[] samples = new double[width * height * 3];

    int spp = in.readInt();
    long renderTime = in.readLong();

    for (int x = 0; x < width; ++x) {
      for (int y = 0; y < height; ++y) {
        samples[(y * width + x) * 3 + 0] = in.readDouble();
        samples[(y * width + x) * 3 + 1] = in.readDouble();
        samples[(y * width + x) * 3 + 2] = in.readDouble();
      }
    }

    return new ChunkyRenderDump(width, height, samples, spp, renderTime);
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public double[] getSamples() {
    return samples;
  }

  /**
   * Saves this render dump.
   *
   * @param dumpFile file to save this dump to
   * @throws IOException if saving the dump fails
   */
  public void saveDump(File dumpFile) throws IOException {
    try (DataOutputStream out = new DataOutputStream(
        new GZIPOutputStream(new FileOutputStream(dumpFile)))) {
      writeDump(out);
    }
  }

  /**
   * Writes this render dump to the given output stream.
   *
   * @param out output stream
   * @throws IOException if writing the dump fails
   */
  public void writeDump(DataOutputStream out) throws IOException {
    out.writeInt(width);
    out.writeInt(height);
    out.writeInt(spp);
    out.writeLong(renderTime);

    for (int x = 0; x < width; ++x) {
      for (int y = 0; y < height; ++y) {
        out.writeDouble(samples[(y * width + x) * 3 + 0]);
        out.writeDouble(samples[(y * width + x) * 3 + 1]);
        out.writeDouble(samples[(y * width + x) * 3 + 2]);
      }
    }
  }

  /**
   * Merges a render dump into this dump.
   *
   * @param dumpFile the file to merge into this dump
   */
  public synchronized void mergeDump(File dumpFile) throws IOException, InterruptedException {
    mergeDump(ChunkyRenderDump.loadFile(dumpFile));
  }

  public synchronized void mergeDump(ChunkyRenderDump dumpToMerge)
      throws IOException, InterruptedException {
    int dumpWidth = dumpToMerge.getWidth();
    int dumpHeight = dumpToMerge.getHeight();
    int dumpSpp = dumpToMerge.getSpp();
    long dumpRenderTime = dumpToMerge.getRenderTime();

    if (dumpWidth != width || dumpHeight != height) {
      throw new IOException("Dump has invalid size");
    }

    double sa = spp / (double) (spp + dumpSpp);
    double sb = 1 - sa;

    for (int i = 0; i < samples.length; i++) {
      samples[i] = samples[i] * sa + dumpToMerge.samples[i] * sb;
    }

    spp += dumpSpp;
    renderTime += dumpRenderTime;
  }

  public synchronized BufferedImage getPicture(double exposure, Postprocess postprocessMethod) {
    BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        double r = samples[(y * width + x) * 3 + 0];
        double g = samples[(y * width + x) * 3 + 1];
        double b = samples[(y * width + x) * 3 + 2];

        r *= exposure;
        g *= exposure;
        b *= exposure;

        switch (postprocessMethod) {
          case NONE:
            break;
          case TONEMAP1:
            // http://filmicgames.com/archives/75
            r = Math.max(0, r - 0.004);
            r = (r * (6.2 * r + .5)) / (r * (6.2 * r + 1.7) + 0.06);
            g = Math.max(0, g - 0.004);
            g = (g * (6.2 * g + .5)) / (g * (6.2 * g + 1.7) + 0.06);
            b = Math.max(0, b - 0.004);
            b = (b * (6.2 * b + .5)) / (b * (6.2 * b + 1.7) + 0.06);
            break;
          case TONEMAP2:
            // https://knarkowicz.wordpress.com/2016/01/06/aces-filmic-tone-mapping-curve/
            float aces_a = 2.51f;
            float aces_b = 0.03f;
            float aces_c = 2.43f;
            float aces_d = 0.59f;
            float aces_e = 0.14f;
            r = Math.max(
                Math.min((r * (aces_a * r + aces_b)) / (r * (aces_c * r + aces_d) + aces_e), 1), 0);
            g = Math.max(
                Math.min((g * (aces_a * g + aces_b)) / (g * (aces_c * g + aces_d) + aces_e), 1), 0);
            b = Math.max(
                Math.min((b * (aces_a * b + aces_b)) / (b * (aces_c * b + aces_d) + aces_e), 1), 0);
            break;
          case TONEMAP3:
            // http://filmicgames.com/archives/75
            float hA = 0.15f;
            float hB = 0.50f;
            float hC = 0.10f;
            float hD = 0.20f;
            float hE = 0.02f;
            float hF = 0.30f;
            // This adjusts the exposure by a factor of 16 so that the resulting exposure approximately matches the other
            // post-processing methods. Without this, the image would be very dark.
            r *= 16;
            g *= 16;
            b *= 16;
            r = ((r * (hA * r + hC * hB) + hD * hE) / (r * (hA * r + hB) + hD * hF)) - hE / hF;
            g = ((g * (hA * g + hC * hB) + hD * hE) / (g * (hA * g + hB) + hD * hF)) - hE / hF;
            b = ((b * (hA * b + hC * hB) + hD * hE) / (b * (hA * b + hB) + hD * hF)) - hE / hF;
            float hW = 11.2f;
            float whiteScale =
                1.0f / (((hW * (hA * hW + hC * hB) + hD * hE) / (hW * (hA * hW + hB) + hD * hF))
                    - hE / hF);
            r *= whiteScale;
            g *= whiteScale;
            b *= whiteScale;
            break;
          case GAMMA:
            r = Math.pow(r, 1 / DEFAULT_GAMMA);
            g = Math.pow(g, 1 / DEFAULT_GAMMA);
            b = Math.pow(b, 1 / DEFAULT_GAMMA);
            break;
        }

        r = Math.min(1, r);
        g = Math.min(1, g);
        b = Math.min(1, b);

        bi.setRGB(x, y, getRGB(r, g, b));
      }
    }

    return bi;
  }

  private static int getRGB(double r, double g, double b) {
    return 0xFF000000 |
        ((int) (255 * r + .5) << 16) |
        ((int) (255 * g + .5) << 8) |
        (int) (255 * b + .5);
  }

  /**
   * Gets the samples per pixel of this dump.
   *
   * @return samples per pixel of this dump
   */
  public int getSpp() {
    return spp;
  }

  /**
   * Gets the total number of samples of this dump.
   *
   * @return total number of samples of this dump
   */
  public int getTotalSamples() {
    return spp * width * height;
  }

  /**
   * Gets the total render time of this dump.
   *
   * @return total render time
   */
  public long getRenderTime() {
    return renderTime;
  }

  public enum Postprocess {
    NONE,
    TONEMAP1,
    TONEMAP2,
    TONEMAP3,
    GAMMA
  }
}
