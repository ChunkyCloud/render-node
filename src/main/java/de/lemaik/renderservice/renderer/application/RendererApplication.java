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

package de.lemaik.renderservice.renderer.application;

import de.lemaik.renderservice.renderer.Main;
import de.lemaik.renderservice.renderer.rendering.RenderServerApiClient;
import de.lemaik.renderservice.renderer.rendering.RenderWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.llbit.chunky.JsonSettings;
import se.llbit.chunky.PersistentSettings;
import se.llbit.chunky.main.Version;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class RendererApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(RendererApplication.class);

    private final RenderServerApiClient api;
    private final RendererSettings settings;
    private Path jobDirectory;
    private Path resourcePacksPath;

    private RenderWorker worker;

    public RendererApplication(RendererSettings settings) {
        this.settings = settings;
        api = new RenderServerApiClient(
                settings.getApiUrl(), settings.getApiKey(),
                settings.getCacheDirectory()
                        .orElse(Paths.get(System.getProperty("user.dir"), "rs_cache").toFile()),
                settings.getMaxCacheSize().orElse(512L)
        );
    }

    public void start() {
        LOGGER.info("Render node version: " + Main.VERSION + " (version code " + Main.VERSION_CODE + ")");
        LOGGER.info("Chunky version: " + Version.getVersion());
        if (getSettings().getJobPath().isPresent()) {
            jobDirectory = getSettings().getJobPath().get().toPath();
        } else {
            jobDirectory = Paths.get(System.getProperty("user.dir"), "rs_jobs");
        }
        LOGGER.info("Job path: " + jobDirectory);
        jobDirectory.toFile().mkdirs();

        Path chunkyHome = Paths.get(System.getProperty("user.dir"), "rs_chunky");
        chunkyHome.toFile().mkdirs();

        PersistentSettings.changeSettingsDirectory(chunkyHome.toFile());
        PersistentSettings.settings = new JsonSettings();
        PersistentSettings.setDisableDefaultTextures(true);
        PersistentSettings.save();

        LOGGER.info("Chunky home: " + chunkyHome);

        if (getSettings().getTexturepacksPath().isPresent()) {
            resourcePacksPath = getSettings().getTexturepacksPath().get().toPath();
        } else {
            resourcePacksPath = Paths.get(System.getProperty("user.dir"), "rs_texturepacks");
        }
        LOGGER.info("Resource packs path: " + resourcePacksPath);
        resourcePacksPath.toFile().mkdirs();

        worker = new RenderWorker(getSettings().getThreads().orElse(Runtime.getRuntime().availableProcessors()),
                getSettings().getCpuLoad().orElse(100),
                getSettings().getName().orElse(null), jobDirectory, resourcePacksPath, api);
        worker.start();
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
}
