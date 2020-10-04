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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import de.lemaik.renderservice.renderer.chunky.ChunkyWrapper;
import de.lemaik.renderservice.renderer.util.FileUtil;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AssignmentWorker implements Runnable {

  private static final Logger LOGGER = LogManager.getLogger(AssignmentWorker.class);
  private static final Gson gson = new Gson();

  private final QueueingConsumer.Delivery delivery;
  private final Channel channel;
  private final Path workingDir;
  private final int threads;
  private final ChunkyWrapper chunky;
  private final RenderServerApiClient apiClient;

  public AssignmentWorker(QueueingConsumer.Delivery delivery, Channel channel, Path workingDir,
      int threads, ChunkyWrapper chunky, RenderServerApiClient apiClient) {
    this.delivery = delivery;
    this.channel = channel;
    this.workingDir = workingDir;
    this.threads = threads;
    this.chunky = chunky;
    this.apiClient = apiClient;
  }

  @Override
  public void run() {
    try {
      Assignment assignment = gson
          .fromJson(new String(delivery.getBody(), "UTF-8"), Assignment.class);
      LOGGER.info(String
          .format("New assignment: %d spp for job %s", assignment.getSpp(), assignment.getJobId()));
      Job job = apiClient.getJob(assignment.getJobId()).get(10, TimeUnit.MINUTES);

      final JsonObject[] sceneDescription = new JsonObject[1];
      LOGGER.info("Downloading scene files...");
      final File skymap = job.getSkymapUrl().isPresent()
          ? apiClient.downloadSkymapTo(job.getSkymapUrl().get(), workingDir).get().getAbsoluteFile()
          : null;

      CompletableFuture.allOf(
          apiClient.getScene(job).thenAccept((scene -> {
            scene.addProperty("name", "scene");
            sceneDescription[0] = scene;
            if (skymap != null) {
              scene.getAsJsonObject("sky").addProperty("skymap", skymap.getAbsolutePath());
            }
            try (OutputStreamWriter out = new OutputStreamWriter(
                new FileOutputStream(new File(workingDir.toFile(), "scene.json")))) {
              new Gson().toJson(scene, out);
            } catch (IOException e) {
              // TODO
              e.printStackTrace();
            }
          })),
          // apiClient.downloadFoliage(job, new File(workingDir.toFile(), "scene.foliage")),
          // apiClient.downloadGrass(job, new File(workingDir.toFile(), "scene.grass")),
          apiClient.downloadOctree(job, new File(workingDir.toFile(), "scene.octree2")),
          apiClient.downloadEmittergrid(job, new File(workingDir.toFile(), "scene.emittergrid"))
      ).get(4, TimeUnit.HOURS); // timeout after 4 hours of downloading

      LOGGER.info("Rendering...");
      byte[] dump = chunky
          .render(null, new File(workingDir.toFile(), "scene.json"), assignment.getSpp(), threads,
              100).get();

      LOGGER.info("Uploading...");
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        // NOTE: GZIPOutputStream must be explicitly closed to finish writing (flushing will not help)
        try (DataOutputStream out = new DataOutputStream(bos)) {
          out.writeUTF(assignment.getJobId());
          out.write(dump);
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
