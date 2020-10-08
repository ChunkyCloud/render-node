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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class RenderServerApiClient {

  private static final Gson gson = new Gson();
  private final String baseUrl;
  private final OkHttpClient client;

  public RenderServerApiClient(String baseUrl, File cacheDirectory, long maxCacheSize) {
    this.baseUrl = baseUrl;
    client = new OkHttpClient.Builder()
        .cache(new Cache(cacheDirectory, maxCacheSize))
        .build();
  }

  public CompletableFuture<RenderServiceInfo> getInfo() {
    CompletableFuture<RenderServiceInfo> result = new CompletableFuture<>();

    client.newCall(new Request.Builder()
        .url(baseUrl + "/info").get().build())
        .enqueue(new Callback() {
          @Override
          public void onFailure(Call call, IOException e) {
            result.completeExceptionally(e);
          }

          @Override
          public void onResponse(Call call, Response response) {
            if (response.code() == 200) {
              try (InputStreamReader reader = new InputStreamReader(response.body().byteStream())) {
                result.complete(gson.fromJson(reader, RenderServiceInfo.class));
              } catch (IOException e) {
                result.completeExceptionally(e);
              }
            } else {
              result.completeExceptionally(
                  new IOException("The render service info could not be downloaded"));
            }
          }
        });

    return result;
  }

  public CompletableFuture<Job> getJob(String jobId) {
    CompletableFuture<Job> result = new CompletableFuture<>();

    client.newCall(new Request.Builder()
        .url(baseUrl + "/jobs/" + jobId).get().build())
        .enqueue(new Callback() {
          @Override
          public void onFailure(Call call, IOException e) {
            result.completeExceptionally(e);
          }

          @Override
          public void onResponse(Call call, Response response) {
            if (response.code() == 200) {
              try (InputStreamReader reader = new InputStreamReader(response.body().byteStream())) {
                result.complete(gson.fromJson(reader, Job.class));
              } catch (IOException e) {
                result.completeExceptionally(e);
              }
            } else {
              result.completeExceptionally(new IOException("The job could not be downloaded"));
            }
          }
        });

    return result;
  }

  public CompletableFuture<JsonObject> getScene(Job job) {
    CompletableFuture<JsonObject> result = new CompletableFuture<>();

    client.newCall(new Request.Builder()
        .url(baseUrl + job.getSceneUrl()).get().build())
        .enqueue(new Callback() {
          @Override
          public void onFailure(Call call, IOException e) {
            result.completeExceptionally(e);
          }

          @Override
          public void onResponse(Call call, Response response) {
            if (response.code() == 200) {
              try (InputStreamReader reader = new InputStreamReader(response.body().byteStream())) {
                result.complete(gson.fromJson(reader, JsonObject.class));
              } catch (IOException e) {
                result.completeExceptionally(e);
              }
            } else {
              result.completeExceptionally(new IOException("The scene could not be downloaded"));
            }
          }
        });

    return result;
  }

  public CompletableFuture downloadFoliage(Job job, File file) {
    return downloadFile(baseUrl + job.getFoliageUrl(), file);
  }

  public CompletableFuture downloadGrass(Job job, File file) {
    return downloadFile(baseUrl + job.getGrassUrl(), file);
  }

  public CompletableFuture downloadOctree(Job job, File file) {
    return downloadFile(baseUrl + job.getOctreeUrl(), file);
  }

  public CompletableFuture downloadEmittergrid(Job job, File file) {
    Optional<String> url = job.getEmittergridUrl();
    return url.map(s -> downloadFile(baseUrl + s, file))
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

              try (BufferedSink sink = Okio.buffer(Okio.sink(file))) {
                sink.writeAll(response.body().source());
                result.complete(file);
              } catch (IOException e) {
                result.completeExceptionally(e);
              }
            } else {
              result.completeExceptionally(new IOException("Download of " + url + " failed"));
            }
          }
        });

    return result;
  }

  private CompletableFuture downloadFile(String url, File file) {
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
            if (response.code() == 200) {
              try (BufferedSink sink = Okio.buffer(Okio.sink(file))) {
                sink.writeAll(response.body().source());
                result.complete(file);
              } catch (IOException e) {
                result.completeExceptionally(e);
              }
            } else {
              result.completeExceptionally(new IOException("Download of " + url + " failed"));
            }
          }
        });

    return result;
  }
}