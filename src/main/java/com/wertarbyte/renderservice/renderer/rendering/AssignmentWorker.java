package com.wertarbyte.renderservice.renderer.rendering;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import com.wertarbyte.renderservice.libchunky.ChunkyWrapper;
import com.wertarbyte.renderservice.libchunky.RenderListenerAdapter;
import com.wertarbyte.renderservice.libchunky.scene.SceneDescription;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

public class AssignmentWorker implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(AssignmentWorker.class);
    private static final Gson gson = new Gson();

    private final QueueingConsumer.Delivery delivery;
    private final Channel channel;
    private final Path workingDir;
    private final ChunkyWrapper chunky;

    public AssignmentWorker(QueueingConsumer.Delivery delivery, Channel channel, Path workingDir, ChunkyWrapper chunky) {
        this.delivery = delivery;
        this.channel = channel;
        this.workingDir = workingDir;
        this.chunky = chunky;
    }

    @Override
    public void run() {
        try {
            Assignment assignment = gson.fromJson(new String(delivery.getBody(), "UTF-8"), Assignment.class);
            LOGGER.info(String.format("New assignment: %d spp for job %s", assignment.getSpp(), assignment.getJobId()));

            LOGGER.info("Downloading scene...");
            SceneDescription sceneDescription;
            try (Response response = new RenderServerApiClient("http://localhost:3000", new File("/tmp"), 1024 * 1024 * 1024)
                    .getScene(assignment.getJobId())
                    .get()) {
                if (!response.isSuccessful()) {
                    LOGGER.warn("Downloading the scene failed");

                    try {
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                    } catch (IOException e) {
                        LOGGER.error("Could not nack a failed task", e);
                    }
                    return;
                }

                // TODO download foliage, grass and octree

                try (InputStreamReader reader = new InputStreamReader(response.body().byteStream())) {
                    sceneDescription = new SceneDescription(gson.fromJson(reader, JsonObject.class));
                    sceneDescription.setName("scene");
                }
            }

            LOGGER.info("Rendering...");
            chunky.setScene(sceneDescription);
            chunky.setSceneDirectory(workingDir.toFile());
            chunky.setTargetSpp(assignment.getSpp());
            chunky.addListener(new RenderListenerAdapter() {
                @Override
                public void onRenderStatusChanged(int currentSpp, int targetSpp) {
                    LOGGER.info(String.format("%d of %d SPP (%.2f%%; %.0f SPS)", currentSpp, targetSpp, currentSpp * 100f / targetSpp, chunky.getSamplesPerSecond()));
                }
            });
            chunky.render();

            LOGGER.info("Uploading...");
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                // NOTE: GZIPOutputStream must be explicitly closed to finish writing (flushing will not help)
                try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bos))) {
                    out.writeUTF(assignment.getJobId());
                    chunky.getDump().writeDump(out);
                }
                channel.basicPublish("", "rs_dumps", MessageProperties.PERSISTENT_BASIC, bos.toByteArray());
            }

            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            LOGGER.info("Done");
        } catch (Exception e) {
            LOGGER.warn("An error occurred while processing a task", e);

            try {
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            } catch (IOException e1) {
                LOGGER.error("Could not nack a failed task", e);
            }
        }
    }
}
