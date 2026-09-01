package com.fabpharos.minecraftthegathering.scryfall;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;

import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Downloads Scryfall's set list (https://scryfall.com/docs/api/sets) - unlike cards, this is small enough
 * (~1000 entries) to come back as a single response, no bulk-data dump needed - filters it down to the set
 * types this mod cares about, and stores the result under the game directory so every save in the instance
 * can read it. Mirrors {@link ScryfallBulkDataService}'s download-if-missing / manual-redownload pattern.
 */
public final class ScryfallSetService {
    private static final String USER_AGENT = "MinecraftTheGathering-Mod/1.0";
    private static final String SETS_FILE_NAME = "sets.json";

    // Only these set types are worth handing out booster packs for; skip promos, tokens, memorabilia, etc.
    private static final Set<String> ALLOWED_SET_TYPES = Set.of("core", "expansion", "masters", "eternal");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private static final AtomicBoolean DOWNLOAD_IN_PROGRESS = new AtomicBoolean(false);

    private ScryfallSetService() {
    }

    public static Path setsFilePath() {
        Path dir = FMLPaths.getOrCreateGameRelativePath(Paths.get(MinecraftTheGathering.MODID, "carddata"));
        return dir.resolve(SETS_FILE_NAME);
    }

    public static boolean isDownloadInProgress() {
        return DOWNLOAD_IN_PROGRESS.get();
    }

    /** Called on server start. Downloads set data only if it isn't already present, then loads it into memory. */
    public static void ensureSetDataReady(MinecraftServer server) {
        run(server, ScryfallSetService::ensureReadyBlocking, success -> {
            if (!success) {
                MinecraftTheGathering.LOGGER.warn("Local Magic set data isn't ready; it will be retried on next server start.");
            }
        });
    }

    /** Manual trigger, e.g. from a command: always re-downloads and reloads. */
    public static void downloadNow(MinecraftServer server, @Nullable Consumer<Boolean> onComplete) {
        run(server, ScryfallSetService::downloadAndLoadBlocking, onComplete);
    }

    private static void run(MinecraftServer server, BooleanSupplier work, @Nullable Consumer<Boolean> onComplete) {
        if (!DOWNLOAD_IN_PROGRESS.compareAndSet(false, true)) {
            MinecraftTheGathering.LOGGER.info("A Scryfall set data operation is already in progress; ignoring new request.");
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
        Path setsFile = setsFilePath();
        if (Files.isRegularFile(setsFile)) {
            MinecraftTheGathering.LOGGER.info("Scryfall set data already present at {}", setsFile);
            return MagicSetPool.load(setsFile);
        }

        return downloadAndLoadBlocking();
    }

    private static boolean downloadAndLoadBlocking() {
        return downloadBlocking() && MagicSetPool.load(setsFilePath());
    }

    // GET https://api.scryfall.com/sets returns every set Scryfall knows about in one response (no
    // pagination for this endpoint), so this is a single request rather than a streamed bulk download.
    private static boolean downloadBlocking() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.scryfall.com/sets"))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                MinecraftTheGathering.LOGGER.warn("Scryfall sets request failed ({}): {}", response.statusCode(), response.body());
                return false;
            }

            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray filtered = new JsonArray();
            int total = 0;
            for (JsonElement element : body.getAsJsonArray("data")) {
                total++;
                JsonObject set = element.getAsJsonObject();
                if (set.has("set_type") && ALLOWED_SET_TYPES.contains(set.get("set_type").getAsString())) {
                    JsonObject entry = new JsonObject();
                    entry.addProperty("id", set.get("id").getAsString());
                    entry.addProperty("code", set.get("code").getAsString());
                    entry.addProperty("name", set.get("name").getAsString());
                    entry.addProperty("set_type", set.get("set_type").getAsString());
                    filtered.add(entry);
                }
            }

            Path target = setsFilePath();
            Path tempFile = target.resolveSibling(target.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                new Gson().toJson(filtered, writer);
            }

            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            MinecraftTheGathering.LOGGER.info("Saved {} Magic sets (out of {} total) to {}", filtered.size(), total, target);
            return true;
        } catch (IOException e) {
            MinecraftTheGathering.LOGGER.warn("Failed to download Scryfall set data", e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
