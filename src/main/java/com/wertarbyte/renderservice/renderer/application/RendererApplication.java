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

package com.wertarbyte.renderservice.renderer.application;

import com.wertarbyte.renderservice.libchunky.ChunkyProcessWrapper;
import com.wertarbyte.renderservice.renderer.rendering.RenderWorker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

public abstract class RendererApplication {
    private static final Logger LOGGER = LogManager.getLogger(RendererApplication.class);
    private static final String SETTINGS_FILENAME = "serverSettings.yml";

    //private final RenderServerApiClient api;
    private final RendererSettings settings;
    private final Path jobDirectory;
    private final ChunkyWrapperFactory chunkyWrapperFactory;

    private RenderWorker worker;
    private UUID id = UUID.randomUUID();
    private File texturepackPath;
    private Optional<UUID> account;

    public RendererApplication(RendererSettings settings) {
        this.settings = settings;
        //api = new RenderServerApiClient(baseUrl, cacheDirectory, maxCacheSize);

        /*
        try {
            texturepackPath = TempFileUtil.extractTemporary(BundledResourceManager.getDefaultTextures());
            LOGGER.info("Temporary textures path: " + texturepackPath.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Could not extract textures", e);
            System.exit(1);
        }
        */
        // TODO

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
            // chunky.setTexturepack(texturepackPath); //TODO
            return chunky;
        };
    }

    public void start() {
        worker = new RenderWorker("172.17.0.3", 5672, getSettings().getProcesses().orElse(1), jobDirectory, chunkyWrapperFactory);
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
