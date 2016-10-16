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

package com.wertarbyte.renderservice.renderer.util;

import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * A utility class to download Minecraft jars.
 */
public class MinecraftDownloader {
    public static CompletableFuture<Response> downloadMinecraft(String version) {
        CompletableFuture<Response> result = new CompletableFuture<Response>();
        new OkHttpClient.Builder().build()
                .newCall(new Request.Builder()
                        .url(String.format("https://s3.amazonaws.com/Minecraft.Download/versions/%1$s/%1$s.jar", version))
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
}
