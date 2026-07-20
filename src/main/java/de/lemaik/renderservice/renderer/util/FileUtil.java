/*
/*
 * Copyright (C) 2013-2026 leMaik and contributors
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

package de.lemaik.renderservice.renderer.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Contains static file utility methods.
 */
public class FileUtil {

    /**
     * Deletes the given directory recursively.
     *
     * @param directory directory to delete
     * @throws IOException if deleting the directory failed
     */
    public static void deleteDirectory(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            List<Path> pathsToDelete = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : pathsToDelete) {
                Files.deleteIfExists(path);
            }
        }
    }
}
