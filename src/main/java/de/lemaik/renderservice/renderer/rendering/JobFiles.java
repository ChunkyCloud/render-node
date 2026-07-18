package de.lemaik.renderservice.renderer.rendering;

public class JobFiles {
    private File scene;
    private File octree;
    private File emittergrid;

    public File getScene() {
        return scene;
    }

    public File getOctree() {
        return octree;
    }

    public File getEmittergrid() {
        return emittergrid;
    }

    public static class File {
        private String url;

        public String getUrl() {
            return url;
        }
    }
}
