package com.wertarbyte.renderservice.renderer.rendering;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.wertarbyte.renderservice.renderer.application.ChunkyWrapperFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * A renderer worker thread.
 */
public class RenderWorker extends Thread {
    private static final Logger LOGGER = LogManager.getLogger(RenderWorker.class);
    private final ExecutorService executorService;
    private final Path jobDirectory;
    private final ChunkyWrapperFactory chunkyFactory;
    private final int poolSize;
    private final int MAX_RESTART_DELAY_SECONDS = 1800; // 30 minutes
    private int nextRestartDelaySeconds = 5;
    private ConnectionFactory factory;
    private Connection conn;
    private Channel channel;

    public RenderWorker(String host, int port, int poolSize, Path jobDirectory, ChunkyWrapperFactory chunkyFactory) {
        this.poolSize = poolSize;
        executorService = Executors.newFixedThreadPool(poolSize);
        this.jobDirectory = jobDirectory;
        this.chunkyFactory = chunkyFactory;
        factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
    }

    @Override
    public void run() {
        while (!interrupted()) {
            LOGGER.info("Starting, using up to " + poolSize + " concurrent render processes");

            try {
                connect();
                QueueingConsumer consumer = new QueueingConsumer(channel);
                channel.basicQos(poolSize, false); // only fetch <poolSize> tasks at once
                channel.basicConsume("rs_tasks", false, consumer);

                while (!interrupted()) {
                    try {
                        Path assignmentPath = jobDirectory.resolve(UUID.randomUUID().toString());
                        assignmentPath.toFile().mkdir();
                        executorService.submit(new AssignmentWorker(consumer.nextDelivery(), channel, assignmentPath, chunkyFactory.getChunkyInstance()));
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
                    nextRestartDelaySeconds = Math.min(MAX_RESTART_DELAY_SECONDS, nextRestartDelaySeconds + 10);
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
