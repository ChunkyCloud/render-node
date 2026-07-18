/*
 * Copyright (C) 2026 leMaik and contributors
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

package de.lemaik.renderservice.renderer.rendering;

import java.util.List;

public class JobFiles {
    private File scene;
    private File octree;
    private File emittergrid;
    private List<ResourcePack> resourcePacks;

    public File getScene() {
        return scene;
    }

    public File getOctree() {
        return octree;
    }

    public File getEmittergrid() {
        return emittergrid;
    }

    public List<ResourcePack> getResourcePacks() {
        return resourcePacks;
    }

    public static class File {
        private String url;

        public String getUrl() {
            return url;
        }
    }

    public static class ResourcePack {
        private int id;
        private String url;

        public int getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }
    }
}
