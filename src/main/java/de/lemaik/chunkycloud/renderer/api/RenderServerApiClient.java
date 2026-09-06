/*
 * Copyright (C) 2016-2026 leMaik and contributors
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

package de.lemaik.chunkycloud.renderer.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.lemaik.chunkycloud.renderer.Main;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import se.llbit.chunky.main.Version;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RenderServerApiClient {

    private static final Gson gson = new Gson();
    private final String baseUrl;
    private final OkHttpClient client;
    private final OkHttpClient uploadClient;

    public RenderServerApiClient(String baseUrl, String apiKey, File cacheDirectory,
                                 long maxCacheSize) {
        this.baseUrl = baseUrl;
        client = new OkHttpClient.Builder()
                .followRedirects(true)
                .cache(new Cache(cacheDirectory, maxCacheSize))
                .addInterceptor(chain -> {
                    if (chain.request().url().toString().startsWith(baseUrl)) {
                        return chain.proceed(
                                chain.request().newBuilder()
                                        .header("User-Agent",
                                                "ChunkyCloudRenderNode/" + Main.VERSION + " (VC " + Main.VERSION_CODE + ") Chunky/" + Version.getVersion() + " (" + Version.getCommit() + ")")
                                        .header("Authorization", "Bearer " + apiKey)
                                        .build()
                        );
                    }
                    return chain.proceed(chain.request());
                })
                .connectTimeout(
                        Integer.parseInt(System.getProperty("chunkycloud.http.connectTimeout", "10")),
                        TimeUnit.SECONDS)
                .writeTimeout(Integer.parseInt(System.getProperty("chunkycloud.http.writeTimeout", "10")),
                        TimeUnit.SECONDS)
                .readTimeout(Integer.parseInt(System.getProperty("chunkycloud.http.readTimeout", "35")), // TODO
                        TimeUnit.SECONDS)
                .build();
        uploadClient = client.newBuilder()
                .connectTimeout(
                        Integer.parseInt(System.getProperty("chunkycloud.http.uploadConnectTimeout", "10")),
                        TimeUnit.SECONDS)
                .writeTimeout(
                        Integer.parseInt(System.getProperty("chunkycloud.http.uploadWriteTimeout", "1800")),
                        TimeUnit.SECONDS)
                .readTimeout(
                        Integer.parseInt(System.getProperty("chunkycloud.http.uploadReadTimeout", "10")),
                        TimeUnit.SECONDS)
                .build();
    }

    public Task getNextTask() throws IOException {
        // TODO add scheduler hints
        Request request = new Request.Builder()
                .url(baseUrl + "/nodes/me/tasks/next")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                try (
                        ResponseBody body = requireBody(response, "task");
                        InputStreamReader reader = new InputStreamReader(body.byteStream())
                ) {
                    return gson.fromJson(reader, Task.class);
                }
            } else if (response.code() == 204) {
                return null;
            }
            throw new IOException("The job could not be downloaded, status " + response.code());
        }
    }

    public FinishTaskRenderingResponse finishTaskRendering(int taskId) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/nodes/me/tasks/" + taskId + "/upload")
                .post(RequestBody.EMPTY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                try (
                        ResponseBody body = requireBody(response, "task upload response");
                        InputStreamReader reader = new InputStreamReader(body.byteStream())
                ) {
                    return gson.fromJson(reader, FinishTaskRenderingResponse.class);
                }
            }
            throw new IOException("The task could not be finished, status " + response.code());
        }
    }

    public void finishTask(int taskId) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/nodes/me/tasks/" + taskId + "/finish")
                .post(RequestBody.EMPTY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("The task could not be finished, status " + response.code());
            }
        }
    }

    public ProgressReportResult reportTaskProgress(int taskId, int spp, double sps) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/nodes/me/tasks/" + taskId + "/progress")
                .post(RequestBody.create(
                        gson.toJson(Map.of("spp", spp, "sps", sps)),
                        MediaType.parse("application/json")
                ))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return ProgressReportResult.OK;
            } else if (response.code() == 409) {
                return ProgressReportResult.STOP_RENDERING;
            }
            throw new IOException("The task progress could not be updated, status " + response.code());
        }
    }

    public JsonObject getScene(Task job) throws IOException {
        JobFiles.File sceneFile = job.getFiles().getScene();
        Request request = new Request.Builder()
                .url(resolveUrl(sceneFile.getUrl()))
                .removeHeader("Authorization")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                try (
                        ResponseBody body = requireBody(response, "scene");
                        InputStreamReader reader = new InputStreamReader(body.byteStream())
                ) {
                    return gson.fromJson(reader, JsonObject.class);
                }
            }
            throw new IOException("The scene could not be downloaded, status " + response.request().url() + " " + response.code());
        }
    }

    protected String resolveUrl(String relativeOrAbsoluteUrl) {
        return relativeOrAbsoluteUrl.startsWith("/") ? baseUrl + relativeOrAbsoluteUrl : relativeOrAbsoluteUrl;
    }

    public File downloadOctree(Task job, File file) throws IOException {
        return downloadFile(resolveUrl(job.getFiles().getOctree().getUrl()), file);
    }

    public File downloadEmittergrid(Task job, File file) throws IOException {
        if (job.getFiles().getEmittergrid().isEmpty()) {
            return null;
        }
        return downloadFile(resolveUrl(job.getFiles().getEmittergrid().orElseThrow().getUrl()), file);
    }

    public File downloadSkymapTo(String url, Path targetDir) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new IOException("Download of " + url + " failed, status " + response.code());
            }
            String filename = response.header("X-Filename");
            if (filename == null || filename.isBlank()) {
                throw new IOException("Download of " + url + " did not include X-Filename");
            }

            File file = new File(targetDir.toFile(), filename);
            File tmpFile = new File(file.getAbsolutePath() + ".tmp");
            try {
                try (
                        ResponseBody body = requireBody(response, "skymap");
                        BufferedSink sink = Okio.buffer(Okio.sink(tmpFile))
                ) {
                    sink.writeAll(body.source());
                }
                if (!tmpFile.renameTo(file)) {
                    throw new IOException("Could not rename file " + tmpFile + " to " + file);
                }
                return file;
            } catch (IOException e) {
                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
                throw e;
            }
        }
    }

    private File downloadFile(String url, File file) throws IOException {
        File tmpFile = new File(file.getAbsolutePath() + ".tmp");
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new IOException("Download of " + url + " failed, status " + response.code());
            }
            try {
                try (
                        ResponseBody body = requireBody(response, "download");
                        BufferedSink sink = Okio.buffer(Okio.sink(tmpFile))
                ) {
                    sink.writeAll(body.source());
                }
                if (!tmpFile.renameTo(file)) {
                    throw new IOException("Could not rename file " + tmpFile + " to " + file);
                }
                return file;
            } catch (IOException e) {
                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
                throw e;
            }
        }
    }

    public void uploadFile(String url, Buffer body, String mimeType) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .put(RequestBody.create(body.snapshot(), MediaType.parse(mimeType)))
                .build();

        try (Response response = uploadClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Upload failed, status " + response.code());
            }
        }
    }

    private static ResponseBody requireBody(Response response, String description) throws IOException {
        ResponseBody body = response.body();
        if (body == null) {
            throw new IOException("Empty response body for " + description);
        }
        return body;
    }

    public enum ProgressReportResult {
        OK,
        STOP_RENDERING
    }
}
