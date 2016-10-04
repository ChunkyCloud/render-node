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
