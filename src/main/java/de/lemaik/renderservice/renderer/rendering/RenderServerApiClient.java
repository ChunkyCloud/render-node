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

package de.lemaik.renderservice.renderer.rendering;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.lemaik.renderservice.renderer.Main;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
                                                "ChunkyCloud Render Node v" + Main.VERSION + ", VC " + Main.VERSION_CODE)
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

    public CompletableFuture<Task> getNextTask() {
        // TODO add scheduler hints
        CompletableFuture<Task> result = new CompletableFuture<>();
        client.newCall(new Request.Builder()
                        .url(baseUrl + "/nodes/me/tasks/next").get()
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        result.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try (response) {
                            if (response.code() == 200) {
                                try (InputStreamReader reader = new InputStreamReader(response.body().byteStream())) {
                                    result.complete(gson.fromJson(reader, Task.class));
                                } catch (IOException e) {
                                    result.completeExceptionally(e);
                                }
                            } else if (response.code() == 204) {
                                result.complete(null);
                            } else {
                                result.completeExceptionally(new IOException("The job could not be downloaded " + response.code()));
                            }
                        }
                    }
                });

        return result;
    }

    public CompletableFuture<FinishTaskRenderingResponse> finishTaskRendering(int taskId) {
        CompletableFuture<FinishTaskRenderingResponse> result = new CompletableFuture<>();
        client.newCall(new Request.Builder()
                        .url(baseUrl + "/nodes/me/tasks/" + taskId + "/upload").post(RequestBody.create(null, new byte[0]))
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        result.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try (response) {
                            if (response.isSuccessful()) {
                                try (InputStreamReader reader = new InputStreamReader(response.body().byteStream())) {
                                    result.complete(gson.fromJson(reader, FinishTaskRenderingResponse.class));
                                } catch (IOException e) {
                                    result.completeExceptionally(e);
                                }
                            } else {
                                result.completeExceptionally(new IOException("The task could not be finished"));
                            }
                        }
                    }
                });
        return result;
    }

    public CompletableFuture<Void> finishTask(int taskId) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        client.newCall(new Request.Builder()
                        .url(baseUrl + "/nodes/me/tasks/" + taskId + "/finish").post(RequestBody.create(null, new byte[0]))
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        result.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try (response) {
                            if (response.isSuccessful()) {
                                result.complete(null);
                            } else {
                                result.completeExceptionally(new IOException("The task could not be finished, status " + response.code() + " " + response.body().string()));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
        return result;
    }

    public CompletableFuture<ProgressReportResult> reportTaskProgress(int taskId, int spp) {
        CompletableFuture<ProgressReportResult> result = new CompletableFuture<>();
        client.newCall(new Request.Builder()
                        .url(baseUrl + "/nodes/me/tasks/" + taskId + "/progress")
                        .post(RequestBody.create(MediaType.parse("application/json"), "{\"spp\":" + spp + "}"))
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        result.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try (response) {
                            if (response.isSuccessful()) {
                                result.complete(ProgressReportResult.OK);
                            } else if (response.code() == 409) {
                                result.complete(ProgressReportResult.STOP_RENDERING);
                            } else {
                                result.completeExceptionally(new IOException("The task progress could not be updated, status " + response.code() + " " + response.body().string()));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
        return result;
    }

    public CompletableFuture<JsonObject> getScene(Task job) {
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        JobFiles.File sceneFile = job.getFiles().getScene();

        client.newCall(new Request.Builder()
                        .url(resolveUrl(sceneFile.getUrl()))
                        .removeHeader("Authorization")
                        .get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        result.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try (response) {
                            if (response.code() == 200) {
                                try (
                                        ResponseBody body = response.body();
                                        InputStreamReader reader = new InputStreamReader(body.byteStream())
                                ) {
                                    result.complete(gson.fromJson(reader, JsonObject.class));
                                } catch (IOException e) {
                                    result.completeExceptionally(e);
                                }
                            } else {
                                result.completeExceptionally(new IOException("The scene could not be downloaded, status " + response.request().url() + " " + response.body().string()));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        return result;
    }

    protected String resolveUrl(String relativeOrAbsoluteUrl) {
        return relativeOrAbsoluteUrl.startsWith("/") ? baseUrl + relativeOrAbsoluteUrl : relativeOrAbsoluteUrl;
    }

    public CompletableFuture downloadOctree(Task job, File file) {
        return downloadFile(resolveUrl(job.getFiles().getOctree().getUrl()), file);
    }

    public CompletableFuture downloadEmittergrid(Task job, File file) {
        return Optional.ofNullable(job.getFiles().getEmittergrid())
                .map(s -> downloadFile(resolveUrl(s.getUrl()), file))
                .orElseGet(() -> CompletableFuture.completedFuture(null));
    }

    public CompletableFuture<File> downloadSkymapTo(String url, Path targetDir) {
        CompletableFuture<File> result = new CompletableFuture<>();

        client.newCall(new Request.Builder()
                        .url(baseUrl + url).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        result.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        if (response.code() == 200) {
                            File file = new File(targetDir.toFile(), response.header("X-Filename"));
                            File tmpFile = new File(file.getAbsolutePath() + ".tmp");

                            try (
                                    ResponseBody body = response.body();
                                    BufferedSink sink = Okio.buffer(Okio.sink(tmpFile))
                            ) {
                                sink.writeAll(body.source());
                            } catch (IOException e) {
                                if (tmpFile.exists()) {
                                    tmpFile.delete();
                                }
                                result.completeExceptionally(e);
                                return;
                            }
                            try {
                                if (!tmpFile.renameTo(file)) {
                                    throw new IOException("Could not rename file " + tmpFile + " to " + file);
                                }
                                result.complete(file);
                            } catch (IOException e) {
                                if (tmpFile.exists()) {
                                    tmpFile.delete();
                                }
                                result.completeExceptionally(e);
                            }
                        } else {
                            response.close();
                            result.completeExceptionally(new IOException("Download of " + url + " failed"));
                        }
                    }
                });

        return result;
    }

    public CompletableFuture downloadResourcepack(String name, File file) {
        return downloadFile(baseUrl + "/resourcepacks/" + name, file);
    }

    private CompletableFuture downloadFile(String url, File file) {
        File tmpFile = new File(file.getAbsolutePath() + ".tmp");
        CompletableFuture<File> result = new CompletableFuture<>();

        client.newCall(new Request.Builder()
                        .url(url).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        result.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try (response) {
                            if (response.code() == 200) {
                                try (
                                        ResponseBody body = response.body();
                                        BufferedSink sink = Okio.buffer(Okio.sink(tmpFile))
                                ) {
                                    sink.writeAll(body.source());
                                } catch (IOException e) {
                                    if (tmpFile.exists()) {
                                        tmpFile.delete();
                                    }
                                    result.completeExceptionally(e);
                                    return;
                                }
                                try {
                                    if (!tmpFile.renameTo(file)) {
                                        throw new IOException("Could not rename file " + tmpFile + " to " + file);
                                    }
                                    result.complete(file);
                                } catch (IOException e) {
                                    if (tmpFile.exists()) {
                                        tmpFile.delete();
                                    }
                                    result.completeExceptionally(e);
                                }
                            } else {
                                result.completeExceptionally(new IOException("Download of " + url + " failed"));
                            }
                        }
                    }
                });

        return result;
    }

    public CompletableFuture<Void> uploadFile(String url, Buffer body, String mimeType) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        uploadClient.newCall(new Request.Builder()
                        .url(url)
                        .put(RequestBody.create(MediaType.parse(mimeType), body.readByteString()))
                        .build()
                )
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        result.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try (response) {
                            if (response.isSuccessful()) {
                                result.complete(null);
                            } else {
                                result.completeExceptionally(new IOException(
                                        "Upload failed" + response.code() + " " + response.body().string()));
                            }
                        }
                    }
                });
        return result;
    }

    public enum ProgressReportResult {
        OK,
        STOP_RENDERING
    }
}
