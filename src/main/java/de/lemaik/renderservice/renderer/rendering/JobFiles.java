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
