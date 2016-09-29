/*
 * Copyright (c) 2013-2015 Wertarbyte <http://wertarbyte.com>
 *
 * This file is part of Wertarbyte RenderService.
 *
 * Wertarbyte RenderService is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wertarbyte RenderService is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wertarbyte RenderService.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.wertarbyte.renderservice.renderer.rendering;

import com.wertarbyte.renderservice.libchunky.ChunkyRenderDump;
import okhttp3.*;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;

public class RenderServerApiClient {
    private final String baseUrl;
    private final OkHttpClient client;

    public RenderServerApiClient(String baseUrl, File cacheDirectory, long maxCacheSize) {
        this.baseUrl = baseUrl;
        client = new OkHttpClient.Builder()
                .cache(new Cache(cacheDirectory, maxCacheSize))
                .build();
    }

    public CompletableFuture<Response> getScene(String jobId) {
        CompletableFuture<Response> result = new CompletableFuture<>();

        client.newCall(new Request.Builder()
                .url(baseUrl + "/jobs/" + jobId + "/files/scene.json")
                .get()
                .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        result.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        result.complete(response);
                    }
                });

        return result;
    }

    public CompletableFuture<Response> sendDump(String jobId, ChunkyRenderDump dump) throws IOException {
        CompletableFuture<Response> result = new CompletableFuture<>();

        byte[] dumpFile;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bos))) {
            dump.writeDump(out);
            dumpFile = bos.toByteArray();
        }

        client.newCall(new Request.Builder()
                .url(baseUrl + "/jobs/" + jobId)
                .post(new MultipartBody.Builder()
                        .setType(MediaType.parse("multipart/form-data"))
                        .addFormDataPart("dump", "scene.dump", RequestBody.create(MediaType.parse("application/octet-stream"), dumpFile))
                        .build())
                .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        result.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        result.complete(response);
                    }
                });

        return result;
    }
}
