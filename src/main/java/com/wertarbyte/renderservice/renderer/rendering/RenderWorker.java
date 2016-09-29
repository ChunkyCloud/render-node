package com.wertarbyte.renderservice.renderer.rendering;

import com.google.gson.Gson;
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
    private final Gson gson = new Gson();
    private final ExecutorService executorService;
    private final Path jobDirectory;
    private final ChunkyWrapperFactory chunkyFactory;
    private final int poolSize;
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
        LOGGER.info("Starting a worker with up to " + poolSize + " concurrent render processes");

        try {
            connect();
        } catch (IOException e) {
            LOGGER.error("Connecting to RabbitMQ failed", e);
            return;
        }

        QueueingConsumer consumer = new QueueingConsumer(channel);
        try {
            channel.basicQos(poolSize, false); // only fetch <poolSize> tasks at once
            channel.basicConsume("rs_tasks", false, consumer);
        } catch (IOException e) {
            LOGGER.error("An error occurred in the worker loop", e);
        }

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

        try {
            conn.close(5000);
        } catch (IOException e) {
            LOGGER.error("An error occurred while shutting down the worker", e);
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
