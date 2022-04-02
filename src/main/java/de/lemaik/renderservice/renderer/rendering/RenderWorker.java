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

import de.lemaik.renderservice.renderer.chunky.ChunkyWrapperFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.lemaik.renderservice.renderer.message.Message;
import de.lemaik.renderservice.renderer.message.TaskMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A renderer worker thread.
 */
public class RenderWorker extends Thread {

  private static final Logger LOGGER = LogManager.getLogger(RenderWorker.class);
  private static final int MAX_RESTART_DELAY_SECONDS = 15 * 60; // 15 minutes
  private static final int PROTOCOL_VERSION = 0;

  private final String apiKey;
  private final Path jobDirectory;
  private final Path texturepacksDirectory;
  private final ChunkyWrapperFactory chunkyFactory;
  private final int threads;
  private final int cpuLoad;
  private final RenderServerApiClient apiClient;

  private int nextRestartDelaySeconds = 1;

  private final MessageClient.Factory connectionFactory;

  public RenderWorker(URI uri, String apiKey, int threads, int cpuLoad, String name, Path jobDirectory,
                      Path texturepacksDirectory, ChunkyWrapperFactory chunkyFactory,
                      RenderServerApiClient apiClient) {
    this.threads = threads;
    this.cpuLoad = cpuLoad;
    this.texturepacksDirectory = texturepacksDirectory;
    this.jobDirectory = jobDirectory;
    this.chunkyFactory = chunkyFactory;
    this.apiClient = apiClient;
    this.apiKey = apiKey;

    connectionFactory = () -> new MessageClient(uri);
  }

  @Override
  public void run() {
    while (!interrupted()) {
      LOGGER.info("Connecting");
      try {
        MessageClient connection = connectionFactory.connect();

        // Connect
        if (connection.connectBlocking(30, TimeUnit.SECONDS)) {
          try {
            LOGGER.info("Connected");
            nextRestartDelaySeconds = 1;
            renderProtocol(connection);
          } finally {
            connection.close();
          }
        } else {
          LOGGER.info("Timed out while attempting to connect.");
        }
      } catch (Exception e) {
        LOGGER.error("An error occurred in the worker loop", e);
      }

      if (!interrupted()) {
        LOGGER.info("Waiting " + nextRestartDelaySeconds + " seconds before restarting...");
        try {
          // noinspection BusyWait
          Thread.sleep(nextRestartDelaySeconds * 1000L);
          nextRestartDelaySeconds = Math.min(MAX_RESTART_DELAY_SECONDS, nextRestartDelaySeconds * 2);
        } catch (InterruptedException e) {
          LOGGER.warn("Interrupted while sleeping", e);
          return;
        }
      } else {
        return;
      }
    }
  }

  private void renderProtocol(MessageClient connection) throws InterruptedException {
    Message message;

    // Server info
    message = connection.poll(30, TimeUnit.SECONDS);
    if (message.getServerInfo() == null || message.getServerInfo().getProtocolVersion() != 0) {
      throw new RuntimeException("Remote is on an incompatible version!");
    } else {
      LOGGER.info(message.toString());
    }

    // Authenticate
    message = connection.poll(30, TimeUnit.SECONDS);
    if (message.getAuthenticationRequest()) {
      connection.send(Message.authentication(apiKey));
    } else {
      throw new RuntimeException("Remote did not ask for authentication message!");
    }

    // Wait for Ok
    message = connection.poll(30, TimeUnit.SECONDS);
    if (!message.getAuthenticationOk()) {
      throw new RuntimeException("Remote did not respond with authentication success!");
    }

    while (!interrupted() && connection.isOpen()) {
      try {
        Path taskPath = jobDirectory.resolve(UUID.randomUUID().toString());
        if (!taskPath.toFile().mkdir()) {
          throw new RuntimeException("Failed to create task folder.");
        }

        // Ask for a new task
        connection.send(Message.taskGet());

        // Wait for new task
        message = connection.poll();
        TaskMessage task = message.getTask();
        if (task == null) {
          if (connection.isClosed()) {
            return;
          }
          throw new RuntimeException("Remote did not send a new task!");
        }

        // Render the task
        new TaskWorker(task, connection, taskPath, texturepacksDirectory, threads, cpuLoad, chunkyFactory.getChunkyInstance(), apiClient).run();
      } catch (InterruptedException e) {
        LOGGER.info("Worker loop interrupted", e);
        break;
      }
    }
  }
}
