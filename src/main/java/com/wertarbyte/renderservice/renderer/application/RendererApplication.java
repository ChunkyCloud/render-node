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

package com.wertarbyte.renderservice.renderer.application;

import com.wertarbyte.renderservice.libchunky.ChunkyProcessWrapper;
import com.wertarbyte.renderservice.renderer.rendering.RenderServerApiClient;
import com.wertarbyte.renderservice.renderer.rendering.RenderServiceInfo;
import com.wertarbyte.renderservice.renderer.rendering.RenderWorker;
import com.wertarbyte.renderservice.renderer.util.MinecraftDownloader;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public abstract class RendererApplication {
    private static final int VERSION = 1;
    private static final String TEXTURE_VERSION = "1.10";
    private static final Logger LOGGER = LogManager.getLogger(RendererApplication.class);

    private final RenderServerApiClient api;
    private final RendererSettings settings;
    private Path jobDirectory;
    private ChunkyWrapperFactory chunkyWrapperFactory;

    private RenderWorker worker;
    private UUID id = UUID.randomUUID();
    private File texturepackPath;

    public RendererApplication(RendererSettings settings) {
        this.settings = settings;
        api = new RenderServerApiClient(
                settings.getMasterApiUrl(),
                settings.getCacheDirectory().orElse(Paths.get(System.getProperty("user.dir"), "rs_cache").toFile()),
                settings.getMaxCacheSize().orElse(512L)
        );
    }

    public void start() {
        RenderServiceInfo rsInfo;
        try {
            rsInfo = api.getInfo().get();
        } catch (Exception e) {
            LOGGER.error("Could not fetch render service info", e);
            System.exit(-1);
            return;
        }

        if (rsInfo.getVersion() > VERSION) {
            LOGGER.error("Update required. The minimum required version is " + rsInfo.getVersion() + ", your version is " + VERSION + ".");
            System.exit(-42);
            return;
        }


        try (Response response = MinecraftDownloader.downloadMinecraft(TEXTURE_VERSION).get()) {
            texturepackPath = File.createTempFile("minecraft", ".jar");
            LOGGER.info("Downloading Minecraft " + TEXTURE_VERSION + " to " + texturepackPath.getAbsolutePath());

            try (BufferedSink sink = Okio.buffer(Okio.sink(texturepackPath))) {
                sink.writeAll(response.body().source());
            }
        } catch (Exception e) {
            LOGGER.error("Could not download assets", e);
            System.exit(-1);
            return;
        }
        LOGGER.info("Finished downloading");

        if (getSettings().getJobPath().isPresent()) {
            jobDirectory = getSettings().getJobPath().get().toPath();
        } else {
            jobDirectory = Paths.get(System.getProperty("user.dir"), "rs_jobs");
            LOGGER.warn("No job path specified, using " + jobDirectory.toString());
        }
        jobDirectory.toFile().mkdirs();

        chunkyWrapperFactory = () -> {
            ChunkyProcessWrapper chunky = new ChunkyProcessWrapper();
            chunky.setThreadCount(getSettings().getThreads().orElse(2));
            chunky.setJvmMinMemory(getSettings().getXms().orElse(1024));
            chunky.setJvmMaxMemory(getSettings().getXmx().orElse(2048));
            chunky.setTexturepack(texturepackPath);
            return chunky;
        };

        worker = new RenderWorker(rsInfo.getRabbitMq(), getSettings().getProcesses().orElse(1), getSettings().getName().orElse(null), jobDirectory, chunkyWrapperFactory, api);
        worker.start();
    }

    public UUID getId() {
        return id;
    }

    public RendererSettings getSettings() {
        return settings;
    }

    public void stop() {
        try {
            LOGGER.info("Waiting for worker to stop...");
            worker.interrupt();
            worker.join();
            LOGGER.info("Worker stopped");
        } catch (InterruptedException e) {
            LOGGER.error("Could not gracefully stop the renderer");
        }
    }

    protected abstract void onUpdateAvailable();
}
