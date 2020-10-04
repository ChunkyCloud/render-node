/*
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

package de.lemaik.renderservice.renderer.util;

import java.io.File;

/**
 * Contains static file utility methods.
 */
public class FileUtil {

  /**
   * Deletes the given directory recursively.
   *
   * @param directory directory to delete
   * @return total number of files that were removed
   */
  public static int deleteDirectory(File directory) {
    int count = 0;
    File[] contents = directory.listFiles();
    if (contents != null) {
      for (File f : contents) {
        count += deleteDirectory(f);
      }
    }
    if (directory.delete()) {
      count++;
    }
    return count;
  }
}
