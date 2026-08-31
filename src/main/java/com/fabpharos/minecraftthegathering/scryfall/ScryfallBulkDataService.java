package com.fabpharos.minecraftthegathering.scryfall;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;

import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Owns the local Magic Card data pipeline: download Scryfall's "Default Cards" bulk data dump
 * (https://scryfall.com/docs/api/bulk-data), filter it down to a compact rarity-bucketed pool file
 * (see {@link CardPoolBuilder}), and load that pool into memory (see {@link MagicCardPool}) so the rest
 * of the mod can hand out random cards without ever touching the network.
 * <p>
 * Both the raw dump and the derived pool file live under the game directory - {@code <gamedir>/minecraftthegathering/carddata/}
 * - rather than any particular save, so every world/save in the instance shares the same data.
 */
public final class ScryfallBulkDataService {
    private static final String USER_AGENT = "MinecraftTheGathering-Mod/1.0";
    private static final String BULK_DATA_TYPE = "default_cards";
    private static final String BULK_DATA_FILE_NAME = "default-cards.jsonl.gz";
    private static final String POOL_FILE_NAME = "card-pool.json";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private static final AtomicBoolean DOWNLOAD_IN_PROGRESS = new AtomicBoolean(false);

    private ScryfallBulkDataService() {
    }

    /** The shared, save-independent directory this data lives in. */
    private static Path dataDir() {
        return FMLPaths.getOrCreateGameRelativePath(Paths.get(MinecraftTheGathering.MODID, "carddata"));
    }

    public static Path bulkDataFilePath() {
        return dataDir().resolve(BULK_DATA_FILE_NAME);
    }

    public static Path poolFilePath() {
        return dataDir().resolve(POOL_FILE_NAME);
    }

    public static boolean isDownloadInProgress() {
        return DOWNLOAD_IN_PROGRESS.get();
    }

    /**
     * Called on server start. Downloads the bulk data and/or builds the pool file only for whichever of
     * the two is missing, then (re)loads the pool into memory. A no-op download-wise if both already exist -
     * loading still happens so the in-memory pool is populated for this run.
     */
    public static void ensureCardDataReady(MinecraftServer server) {
        run(server, ScryfallBulkDataService::ensureReadyBlocking, success -> {
            if (!success) {
                MinecraftTheGathering.LOGGER.warn("Local Magic Card data isn't ready; it will be retried on next server start.");
            }
        });
    }

    /** Manual trigger, e.g. from a command: always re-downloads, re-filters, and reloads. */
    public static void downloadNow(MinecraftServer server, @Nullable Consumer<Boolean> onComplete) {
        run(server, ScryfallBulkDataService::redownloadBlocking, onComplete);
    }

    private static void run(MinecraftServer server, BooleanSupplier work, @Nullable Consumer<Boolean> onComplete) {
        if (!DOWNLOAD_IN_PROGRESS.compareAndSet(false, true)) {
            MinecraftTheGathering.LOGGER.info("A Scryfall card data operation is already in progress; ignoring new request.");
            if (onComplete != null) {
                onComplete.accept(false);
            }
            return;
        }

        CompletableFuture
                .supplyAsync(work::getAsBoolean, Util.backgroundExecutor())
                .thenAcceptAsync(success -> {
                    DOWNLOAD_IN_PROGRESS.set(false);
                    if (onComplete != null) {
                        onComplete.accept(success);
                    }
                }, server);
    }

    // Blocking - runs entirely on a background thread.
    private static boolean ensureReadyBlocking() {
        Path bulkFile = bulkDataFilePath();
        if (Files.isRegularFile(bulkFile)) {
            MinecraftTheGathering.LOGGER.info("Scryfall card data already present at {}", bulkFile);
        } else if (!downloadBulkDataBlocking(bulkFile)) {
            return false;
        }

        Path poolFile = poolFilePath();
        if (!Files.isRegularFile(poolFile) && !CardPoolBuilder.buildPoolFile(bulkFile, poolFile)) {
            return false;
        }

        return MagicCardPool.load(poolFile);
    }

    // Blocking - runs entirely on a background thread. Unlike ensureReadyBlocking, always redoes every step.
    private static boolean redownloadBlocking() {
        Path bulkFile = bulkDataFilePath();
        if (!downloadBulkDataBlocking(bulkFile)) {
            return false;
        }

        Path poolFile = poolFilePath();
        if (!CardPoolBuilder.buildPoolFile(bulkFile, poolFile)) {
            return false;
        }

        return MagicCardPool.load(poolFile);
    }

    // Looks up the current download URL for the "default_cards" bulk data type, then streams it straight
    // to disk (it's ~75+ MB compressed, too large to buffer in memory).
    private static boolean downloadBulkDataBlocking(Path target) {
        try {
            String downloadUri = findDefaultCardsDownloadUri();
            if (downloadUri == null) {
                MinecraftTheGathering.LOGGER.warn("Could not find a Scryfall bulk data entry of type \"{}\"", BULK_DATA_TYPE);
                return false;
            }

            MinecraftTheGathering.LOGGER.info("Downloading Scryfall card data from {}", downloadUri);

            Path tempFile = target.resolveSibling(target.getFileName() + ".tmp");

            HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUri))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/octet-stream")
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();

            HttpResponse<Path> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(
                    tempFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));

            if (response.statusCode() != 200) {
                MinecraftTheGathering.LOGGER.warn("Scryfall bulk data download failed with status {}", response.statusCode());
                Files.deleteIfExists(tempFile);
                return false;
            }

            // Same directory as the target, so this is an atomic rename - no risk of a half-written file
            // ever being mistaken for a complete one.
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            MinecraftTheGathering.LOGGER.info("Scryfall card data saved to {} ({} bytes)", target, Files.size(target));
            return true;
        } catch (IOException e) {
            MinecraftTheGathering.LOGGER.warn("Failed to download Scryfall bulk card data", e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // GET https://api.scryfall.com/bulk-data, find the entry whose "type" is "default_cards",
    // and return its current jsonl_download_uri (these rotate, so they can't be hardcoded).
    @Nullable
    private static String findDefaultCardsDownloadUri() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.scryfall.com/bulk-data"))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            MinecraftTheGathering.LOGGER.warn("Scryfall bulk-data listing request failed ({}): {}", response.statusCode(), response.body());
            return null;
        }

        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        for (JsonElement element : body.getAsJsonArray("data")) {
            JsonObject entry = element.getAsJsonObject();
            if (entry.has("type") && BULK_DATA_TYPE.equals(entry.get("type").getAsString())) {
                return entry.has("jsonl_download_uri") ? entry.get("jsonl_download_uri").getAsString() : null;
            }
        }

        return null;
    }
}
