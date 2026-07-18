package de.lemaik.renderservice.renderer.rendering;

public class FinishTaskRenderingResponse {
    private UploadUrls uploadUrls;

    public UploadUrls getUploadUrls() {
        return uploadUrls;
    }

    public static class UploadUrls {
        private String image;
        private String dump;

        public String getImage() {
            return image;
        }

        public String getDump() {
            return dump;
        }
    }
}
