package de.lemaik.renderservice.renderer.rendering;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ResourcePackDownloader {
    private static final ResourcePackDownloader INSTANCE = new ResourcePackDownloader();
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackDownloader.class);

    private final OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(true)
            .connectTimeout(
                    Integer.parseInt(System.getProperty("chunkycloud.http.connectTimeout", "10")),
                    TimeUnit.SECONDS)
            .writeTimeout(Integer.parseInt(System.getProperty("chunkycloud.http.writeTimeout", "10")),
                    TimeUnit.SECONDS)
            .readTimeout(Integer.parseInt(System.getProperty("chunkycloud.http.readTimeout", "10")),
                    TimeUnit.SECONDS)
            .build();

    private final ExecutorService executor =
            Executors.newFixedThreadPool(2);

    public static ResourcePackDownloader getInstance() {
        return INSTANCE;
    }

    public List<Path> downloadResourcePacks(JobFiles jobFiles, Path directory) throws IOException {
        List<Path> files = new ArrayList<>(jobFiles.getResourcePacks().size());
        try {
            Files.createDirectories(directory);

            List<CompletableFuture<Void>> futures = jobFiles.getResourcePacks()
                    .stream()
                    .map(pack -> CompletableFuture.runAsync(() -> {
                        try {
                            files.add(downloadIfNeeded(pack, directory));
                        } catch (IOException e) {
                            throw new CompletionException(e);
                        }
                    }, executor))
                    .toList();

            CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            ).join();

        } finally {
            executor.shutdown();
        }
        return files;
    }

    private Path downloadIfNeeded(JobFiles.ResourcePack pack, Path directory) throws IOException {
        Path target = directory.resolve(pack.getId() + ".zip");
        Path temp = directory.resolve(pack.getId() + ".zip.download");

        // already downloaded
        if (Files.exists(target)) {
            LOGGER.info("Skipping {} because it already exists", pack.getId());
            return target;
        }

        // remove incomplete previous download
        Files.deleteIfExists(temp);

        LOGGER.info("Downloading {}", pack.getId());
        Request request = new Request.Builder()
                .url(pack.getUrl())
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(
                        "HTTP " + response.code() + " while downloading " + pack.getId()
                );
            }
            if (response.body() == null) {
                throw new IOException(
                        "Empty response body for " + pack.getId()
                );
            }
            try (BufferedSink bufferedSink = Okio.buffer(Okio.sink(temp))) {
                bufferedSink.writeAll(response.body().source());
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // fallback for filesystems that don't support atomic moves
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            LOGGER.info("Finished downloading {}", pack.getId());
            return target;
        } catch (Exception e) {
            LOGGER.error("Failed to download {}", pack.getId());
            // cleanup broken download
            Files.deleteIfExists(temp);
            throw e;
        }
    }
}