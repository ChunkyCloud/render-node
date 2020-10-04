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

/**
 * Listener for render jobs.
 */
public interface RenderListener {

  /**
   * Gets called when the job has finished.
   */
  default void onFinished() {
  }

  /**
   * Gets called when the rendering status changes.
   *
   * @param currentSpp current samples per pixel
   * @param targetSpp  target samples per pixel
   */
  default void onRenderStatusChanged(int currentSpp, int targetSpp) {
  }
}
