/*
 * Copyright (c) 2013-2016 Maik Marschner <https://lemaik.de>
 *
 * This file is part of libchunky.
 *
 * libchunky is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * libchunky is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with libchunky.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.lemaik.renderservice.renderer.chunky;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * A wrapper for chunky.
 */
public interface ChunkyWrapper {

  /**
   * Starts the wrapped chunky instance.
   *
   * @throws IOException if an error occures while rendering
   */
  CompletableFuture<byte[]> render(File texturepack, File scene, int targetSpp, int threads,
      int cpuLoad) throws IOException, InterruptedException;

  /**
   * Stops the wrapped chunky instance.
   */
  void stop();

  /**
   * Adds the given listener.
   *
   * @param listener listener to add
   */
  void addListener(RenderListener listener);

  /**
   * Removes the given listener.
   *
   * @param listener listener to remove
   */
  void removeListener(RenderListener listener);

  /**
   * Sets the target samples per second.
   *
   * @param targetSpp target samples per second
   */
  void setTargetSpp(int targetSpp);

  /**
   * Sets the number of threads to use for rendering.
   *
   * @param threadCount number of rendering threads
   */
  void setThreadCount(int threadCount);

  void setDefaultTexturepack(File texturepackPath);
}
