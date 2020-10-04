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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import de.lemaik.renderservice.renderer.chunky.ChunkyWrapperFactory;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A renderer worker thread.
 */
public class RenderWorker extends Thread {

  private static final Logger LOGGER = LogManager.getLogger(RenderWorker.class);
  private final ExecutorService executorService;
  private final Path jobDirectory;
  private final ChunkyWrapperFactory chunkyFactory;
  private final int threads;
  private final int MAX_RESTART_DELAY_SECONDS = 1800; // 30 minutes
  private final RenderServerApiClient apiClient;
  private int nextRestartDelaySeconds = 5;
  private ConnectionFactory factory;
  private Connection conn;
  private Channel channel;

  public RenderWorker(String uri, int threads, String name, Path jobDirectory,
      ChunkyWrapperFactory chunkyFactory, RenderServerApiClient apiClient) {
    this.threads = threads;
    executorService = Executors.newFixedThreadPool(1);
    this.jobDirectory = jobDirectory;
    this.chunkyFactory = chunkyFactory;
    this.apiClient = apiClient;
    factory = new ConnectionFactory();
    try {
      factory.setUri(uri);
    } catch (URISyntaxException | NoSuchAlgorithmException | KeyManagementException e) {
      throw new IllegalArgumentException("Invalid RabbitMQ URI", e);
    }

    Map<String, Object> connectionProps = factory.getClientProperties();
    if (name != null) {
      connectionProps.put("x-rs-name", name);
    }
    factory.setClientProperties(connectionProps);
  }

  @Override
  public void run() {
    while (!interrupted()) {
      LOGGER.info("Starting");
      try {
        connect();

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicQos(1, false); // only fetch <poolSize> tasks at once
        channel.basicConsume("rs_tasks", false, consumer);

        while (!interrupted()) {
          try {
            Path assignmentPath = jobDirectory.resolve(UUID.randomUUID().toString());
            assignmentPath.toFile().mkdir();
            executorService.submit(
                new AssignmentWorker(consumer.nextDelivery(), channel, assignmentPath,
                    threads, chunkyFactory.getChunkyInstance(), apiClient));
          } catch (InterruptedException e) {
            LOGGER.info("Worker loop interrupted", e);
            break;
          }
        }
      } catch (Exception e) {
        LOGGER.error("An error occurred in the worker loop", e);
      }

      if (conn != null) {
        try {
          conn.close(5000);
        } catch (IOException e) {
          LOGGER.error("An error occurred while shutting down", e);
        }
      }

      if (!interrupted()) {
        LOGGER.info("Waiting " + nextRestartDelaySeconds + " seconds before restarting...");
        try {
          Thread.sleep(nextRestartDelaySeconds * 1000);
          nextRestartDelaySeconds = Math
              .min(MAX_RESTART_DELAY_SECONDS, nextRestartDelaySeconds + 10);
        } catch (InterruptedException e) {
          LOGGER.warn("Interrupted while sleeping", e);
          return;
        }
      } else {
        return;
      }
    }
  }

  private void connect() throws IOException {
    try {
      conn = factory.newConnection();
      channel = conn.createChannel();
    } catch (TimeoutException e) {
      throw new IOException("Timeout while connecting to RabbitMQ", e);
    }
  }
}
