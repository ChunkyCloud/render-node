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

import java.io.File;
import java.util.Optional;

/**
 * Settings for a {@link RendererApplication}.
 */
public class RendererSettings {

  private Integer cpuLoad;
  private Integer threads;
  private Integer xms;
  private Integer xmx;
  private File jobPath;
  private File texturepacksPath;
  private Integer maxUploadRate;
  private String masterApiUrl;
  private File cacheDirectory;
  private Long maxCacheSize;
  private String name;
  private final String apiKey;

  public RendererSettings(Integer cpuLoad, Integer threads, Integer xms, Integer xmx,
      File jobPath, File texturepacksPath, Integer maxUploadRate,
      String masterApiUrl, File cacheDirectory, Long maxCacheSize,
      String name, String apiKey) {
    this.cpuLoad = cpuLoad;
    this.threads = threads;
    this.xms = xms;
    this.xmx = xmx;
    this.jobPath = jobPath;
    this.texturepacksPath = texturepacksPath;
    this.maxUploadRate = maxUploadRate;
    this.masterApiUrl = masterApiUrl;
    this.cacheDirectory = cacheDirectory;
    this.maxCacheSize = maxCacheSize;
    this.name = name;
    this.apiKey = apiKey;
  }

  public Optional<Integer> getCpuLoad() {
    return Optional.ofNullable(cpuLoad);
  }

  public Optional<Integer> getThreads() {
    return Optional.ofNullable(threads);
  }

  public Optional<Integer> getXms() {
    return Optional.ofNullable(xms);
  }

  public Optional<Integer> getXmx() {
    return Optional.ofNullable(xmx);
  }

  public Optional<File> getJobPath() {
    return Optional.ofNullable(jobPath);
  }

  public Optional<File> getTexturepacksPath() {
    return Optional.ofNullable(texturepacksPath);
  }

  public Optional<Integer> getMaxUploadRate() {
    return Optional.ofNullable(maxUploadRate);
  }

  public String getMasterApiUrl() {
    return masterApiUrl;
  }

  public Optional<File> getCacheDirectory() {
    return Optional.ofNullable(cacheDirectory);
  }

  public Optional<Long> getMaxCacheSize() {
    return Optional.ofNullable(maxCacheSize);
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public String getApiKey() {
    return apiKey;
  }
}
