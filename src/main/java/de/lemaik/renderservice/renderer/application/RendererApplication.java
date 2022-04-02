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

import de.lemaik.renderservice.renderer.chunky.ChunkyWrapper;
import de.lemaik.renderservice.renderer.chunky.ChunkyWrapperFactory;
import de.lemaik.renderservice.renderer.chunky.EmbeddedChunkyWrapper;
import de.lemaik.renderservice.renderer.rendering.RenderServerApiClient;
import de.lemaik.renderservice.renderer.rendering.RenderServiceInfo;
import de.lemaik.renderservice.renderer.rendering.RenderWorker;
import de.lemaik.renderservice.renderer.util.MinecraftDownloader;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.llbit.chunky.PersistentSettings;

public abstract class RendererApplication {

  private static final int VERSION = 3;
  private static final String TEXTURE_VERSION = "1.17.1";
  private static final Logger LOGGER = LogManager.getLogger(RendererApplication.class);

  private final RenderServerApiClient api;
  private final RendererSettings settings;
  private Path jobDirectory;
  private Path texturepacksDirectory;
  private ChunkyWrapperFactory chunkyWrapperFactory;

  private RenderWorker worker;
  private UUID id = UUID.randomUUID();
  private File texturepackPath;

  public RendererApplication(RendererSettings settings) {
    this.settings = settings;
    api = new RenderServerApiClient(
        settings.getMasterApiUrl(), settings.getApiKey(),
        settings.getCacheDirectory()
            .orElse(Paths.get(System.getProperty("user.dir"), "rs_cache").toFile()),
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
      LOGGER.error("Update required. The minimum required version is " + rsInfo.getVersion()
          + ", your version is " + VERSION + ".");
      System.exit(-42);
      return;
    }

    try (Response response = MinecraftDownloader.downloadMinecraft(TEXTURE_VERSION).get()) {
      texturepackPath = File.createTempFile("minecraft", ".jar");
      LOGGER.info(
          "Downloading Minecraft " + TEXTURE_VERSION + " to " + texturepackPath.getAbsolutePath());

      try (
          ResponseBody body = response.body();
          BufferedSink sink = Okio.buffer(Okio.sink(texturepackPath))
      ) {
        sink.writeAll(body.source());
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
    }
    LOGGER.info("Job path: " + jobDirectory.toString());
    jobDirectory.toFile().mkdirs();

    Path chunkyHome = Paths.get(System.getProperty("user.dir"), "rs_chunky");
    chunkyHome.toFile().mkdirs();
    PersistentSettings.changeSettingsDirectory(chunkyHome.toFile());
    PersistentSettings.setDisableDefaultTextures(true);
    LOGGER.info("Chunky home: " + chunkyHome);

    if (getSettings().getTexturepacksPath().isPresent()) {
      texturepacksDirectory = getSettings().getTexturepacksPath().get().toPath();
    } else {
      texturepacksDirectory = Paths.get(System.getProperty("user.dir"), "rs_texturepacks");
    }
    LOGGER.info("Resourcepacks path: " + texturepacksDirectory.toString());
    texturepacksDirectory.toFile().mkdirs();

    chunkyWrapperFactory = () -> {
      ChunkyWrapper chunky = new EmbeddedChunkyWrapper();
      chunky.setDefaultTexturepack(texturepackPath);
      return chunky;
    };

    // Construct the proper queue url with username and password from the api key
    // (username is the first 8 characters of the api key)
    URI queueUri;
    try {
      queueUri = new URI(rsInfo.getWsUrl()).resolve("/rendernode");
    } catch (URISyntaxException e) {
      LOGGER.error("Invalid queue url or api key", e);
      System.exit(-1);
      return;
    }

    worker = new RenderWorker(queueUri, settings.getApiKey(), getSettings().getThreads().orElse(2),
        getSettings().getCpuLoad().orElse(100),
        getSettings().getName().orElse(null), jobDirectory, texturepacksDirectory,
        chunkyWrapperFactory, api);
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
