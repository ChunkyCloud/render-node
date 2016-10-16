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

package com.wertarbyte.renderservice.renderer.rendering;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import com.wertarbyte.renderservice.libchunky.ChunkyWrapper;
import com.wertarbyte.renderservice.libchunky.RenderListenerAdapter;
import com.wertarbyte.renderservice.libchunky.util.FileUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.llbit.chunky.renderer.scene.SceneDescription;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

public class AssignmentWorker implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(AssignmentWorker.class);
    private static final Gson gson = new Gson();

    private final QueueingConsumer.Delivery delivery;
    private final Channel channel;
    private final Path workingDir;
    private final ChunkyWrapper chunky;
    private final RenderServerApiClient apiClient;

    public AssignmentWorker(QueueingConsumer.Delivery delivery, Channel channel, Path workingDir, ChunkyWrapper chunky, RenderServerApiClient apiClient) {
        this.delivery = delivery;
        this.channel = channel;
        this.workingDir = workingDir;
        this.chunky = chunky;
        this.apiClient = apiClient;
    }

    @Override
    public void run() {
        try {
            Assignment assignment = gson.fromJson(new String(delivery.getBody(), "UTF-8"), Assignment.class);
            LOGGER.info(String.format("New assignment: %d spp for job %s", assignment.getSpp(), assignment.getJobId()));
            Job job = apiClient.getJob(assignment.getJobId()).get(10, TimeUnit.MINUTES);

            final SceneDescription[] sceneDescription = new SceneDescription[1];
            LOGGER.info("Downloading scene files...");
            CompletableFuture.allOf(
                    apiClient.getScene(job).thenAccept((scene -> {
                        scene.name = "scene";
                        sceneDescription[0] = scene;
                    })),
                    apiClient.downloadFoliage(job, new File(workingDir.toFile(), "scene.foliage")),
                    apiClient.downloadGrass(job, new File(workingDir.toFile(), "scene.grass")),
                    apiClient.downloadOctree(job, new File(workingDir.toFile(), "scene.octree"))
            ).get(4, TimeUnit.HOURS); // timeout after 4 hours of downloading

            Optional<String> skymapUrl = job.getSkymapUrl();
            if (skymapUrl.isPresent()) {
                sceneDescription[0].sky().loadSkymap(apiClient.downloadSkymapTo(skymapUrl.get(), workingDir).get().getAbsolutePath());
            }

            LOGGER.info("Rendering...");
            chunky.setScene(sceneDescription[0]);
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
        } finally {
            FileUtil.deleteDirectory(workingDir.toFile());
        }
    }
}
