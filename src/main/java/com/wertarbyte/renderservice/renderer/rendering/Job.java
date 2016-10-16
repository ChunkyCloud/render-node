/*
 * rs-rendernode is the worker node software of our RenderService.
 * Copyright (C) 2016 Wertarbyte <https://wertarbyte.com>
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

package com.wertarbyte.renderservice.renderer.rendering;

import java.util.List;
import java.util.Optional;

public class Job {
    private String id;
    private List<JobFile> files;

    public String getId() {
        return id;
    }

    public String getSceneUrl() {
        return getUrl("scene");
    }

    public String getFoliageUrl() {
        return getUrl("foliage");
    }

    public String getGrassUrl() {
        return getUrl("grass");
    }

    public String getOctreeUrl() {
        return getUrl("octree");
    }

    public Optional<String> getSkymapUrl() {
        return files.stream().filter(t -> t.getType().equalsIgnoreCase("skymap")).findFirst().map(JobFile::getUrl);
    }

    private String getUrl(String type) {
        Optional<JobFile> file = files.stream().filter(t -> t.getType().equalsIgnoreCase(type)).findFirst();
        if (file.isPresent()) {
            return file.get().getUrl();
        }
        throw new IllegalStateException("Missing " + type + " url");
    }

    public class JobFile {
        private String type;
        private String url;

        public String getType() {
            return type;
        }

        public String getUrl() {
            return url;
        }
    }
}
