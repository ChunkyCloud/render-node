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

import de.lemaik.renderservice.renderer.chunky.RenderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * A renderer worker thread.
 */
public class RenderWorker extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenderWorker.class);
    private final Path jobDirectory;
    private final Path texturepacksDirectory;
    private final int threads;
    private final int cpuLoad;
    private final int MAX_RESTART_DELAY_SECONDS = 15 * 60; // 15 minutes
    private final RenderServerApiClient apiClient;
    private int nextRestartDelaySeconds = 1;

    public RenderWorker(int threads, int cpuLoad, String name, Path jobDirectory,
                        Path texturepacksDirectory, RenderServerApiClient apiClient) {
        this.threads = threads;
        this.cpuLoad = cpuLoad;
        this.texturepacksDirectory = texturepacksDirectory;
        this.jobDirectory = jobDirectory;
        this.apiClient = apiClient;
    }

    @Override
    public void run() {
        TaskWorker worker = null;
        String previousJobId = null;

        while (!interrupted()) {
            LOGGER.info("Polling for new task");
            try {
                Task task = apiClient.getNextTask().get();
                if (task == null) {
                    Thread.sleep(5000L);
                    continue;
                }
                LOGGER.info("Got task {} for job {}", task.getId(), task.getJob().getId());
                Path taskPath = jobDirectory.resolve("task-" + task.getId());
                taskPath.toFile().mkdir();
                if (worker == null || !task.getJob().getId().equals(previousJobId)) {
                    worker = new TaskWorker(taskPath, texturepacksDirectory, threads, cpuLoad, apiClient);
                    worker.loadScene(task);
                    previousJobId = task.getJob().getId();
                }
                worker.renderScene(task);
                nextRestartDelaySeconds = 1;
            } catch (CancellationException e) {
                LOGGER.info("Task cancelled", e);
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted", e);
                break;
            } catch (ExecutionException | TimeoutException | IOException | RenderException e) {
                LOGGER.error("Error", e);
                try {
                    int delaySeconds = Math.min(MAX_RESTART_DELAY_SECONDS, nextRestartDelaySeconds);
                    LOGGER.info("Waiting {} seconds before trying again", delaySeconds);
                    Thread.sleep(delaySeconds * 1000L);
                } catch (InterruptedException ex) {
                    LOGGER.info("Interrupted", e);
                    break;
                }
                nextRestartDelaySeconds *= 2;
            }

            if (interrupted()) {
                break;
            }
        }
        // TODO notify api that we were interrupted
    }
}
