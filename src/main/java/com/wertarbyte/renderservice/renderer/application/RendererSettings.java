/*
 * Copyright (c) 2013-2016 Wertarbyte <http://wertarbyte.com>
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

import java.io.File;
import java.util.Optional;

/**
 * Settings for a {@link RendererApplication}.
 */
public class RendererSettings {
    private Integer processes;
    private Integer threads;
    private Integer xms;
    private Integer xmx;
    private File jobPath;
    private Integer maxUploadRate;
    private String masterServer;

    public static RendererSettings fromCli(CommandlineArguments arguments) {
        return new RendererSettings(
                arguments.getProcesses(),
                arguments.getThreads(),
                arguments.getXms(),
                arguments.getXmx(),
                arguments.getJobPath(),
                arguments.getMaxUploadRate(),
                arguments.getMasterServer()
        );
    }

    public RendererSettings(int processes, int threads, int xms, int xmx,
                            File jobPath, Integer maxUploadRate, String masterServer) {
        this.processes = processes;
        this.threads = threads;
        this.xms = xms;
        this.xmx = xmx;
        this.jobPath = jobPath;
        this.maxUploadRate = maxUploadRate;
        this.masterServer = masterServer;
    }

    public String getMasterServer() {
        return masterServer;
    }

    public Optional<Integer> getProcesses() {
        return Optional.ofNullable(processes);
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

    public Optional<Integer> getMaxUploadRate() {
        return Optional.ofNullable(maxUploadRate);
    }
}
