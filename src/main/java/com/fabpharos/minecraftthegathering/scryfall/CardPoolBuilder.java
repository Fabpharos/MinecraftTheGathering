package com.fabpharos.minecraftthegathering.scryfall;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;
import com.fabpharos.minecraftthegathering.item.MagicCardData;
import com.fabpharos.minecraftthegathering.item.MagicCardFaces;

/**
 * Filters Scryfall's (large, everything-included) bulk card data dump down to a small, rarity-bucketed
 * file containing only the front/back face data of booster-legal paper cards - the only data
 * {@link MagicCardPool} needs to hand out random cards.
 */
public final class CardPoolBuilder {
    private static final int PROGRESS_LOG_INTERVAL = 20000;
    private static final Gson GSON = new GsonBuilder().create();

    private CardPoolBuilder() {
    }

    /**
     * Blocking - streams and filters the (potentially 600+ MB decompressed) bulk data file and writes a
     * compact pool file in its place. Call only from a background thread.
     */
    public static boolean buildPoolFile(Path bulkDataFile, Path poolFile) {
        Map<MagicCardData.Rarity, List<MagicCardFaces>> buckets = new EnumMap<>(MagicCardData.Rarity.class);
        for (MagicCardData.Rarity rarity : MagicCardData.Rarity.values()) {
            buckets.put(rarity, new ArrayList<>());
        }

        MinecraftTheGathering.LOGGER.info("Filtering Scryfall card data for booster-legal paper cards...");
        int total = 0;

        try (InputStream fileIn = Files.newInputStream(bulkDataFile);
                GZIPInputStream gzipIn = new GZIPInputStream(fileIn);
                BufferedReader reader = new BufferedReader(new InputStreamReader(gzipIn, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                if (!line.isBlank()) {
                    try {
                        JsonObject card = JsonParser.parseString(line).getAsJsonObject();
                        ScryfallCardParser.parseBoosterLegalCard(card)
                                .ifPresent(faces -> buckets.get(faces.front().rarity()).add(faces));
                    } catch (JsonSyntaxException | IllegalStateException e) {
                        // Skip malformed lines rather than aborting the whole pass.
                    }
                }

                if (total % PROGRESS_LOG_INTERVAL == 0) {
                    MinecraftTheGathering.LOGGER.info("Filtered {} Scryfall card data entries so far...", total);
                }
            }
        } catch (IOException e) {
            MinecraftTheGathering.LOGGER.warn("Failed to read Scryfall bulk card data at {}", bulkDataFile, e);
            return false;
        }

        return writePoolFile(poolFile, buckets, total);
    }

    private static boolean writePoolFile(Path poolFile, Map<MagicCardData.Rarity, List<MagicCardFaces>> buckets, int total) {
        JsonObject root = new JsonObject();
        for (MagicCardData.Rarity rarity : MagicCardData.Rarity.values()) {
            JsonArray array = new JsonArray();
            for (MagicCardFaces card : buckets.get(rarity)) {
                JsonObject entry = new JsonObject();
                entry.add("front", writeFace(card.front()));
                if (card.isDoubleFaced()) {
                    entry.add("back", writeFace(card.back()));
                }
                array.add(entry);
            }
            root.add(rarity.getSerializedName(), array);
        }

        Path tempFile = poolFile.resolveSibling(poolFile.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            MinecraftTheGathering.LOGGER.warn("Failed to write the local Magic Card pool file", e);
            return false;
        }

        try {
            Files.move(tempFile, poolFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            MinecraftTheGathering.LOGGER.warn("Failed to save the local Magic Card pool file", e);
            return false;
        }

        int matched = buckets.values().stream().mapToInt(List::size).sum();
        MinecraftTheGathering.LOGGER.info(
                "Built local Magic Card pool: {} booster-legal paper cards out of {} total entries "
                        + "({} common, {} uncommon, {} rare, {} mythic rare)",
                matched, total,
                buckets.get(MagicCardData.Rarity.COMMON).size(),
                buckets.get(MagicCardData.Rarity.UNCOMMON).size(),
                buckets.get(MagicCardData.Rarity.RARE).size(),
                buckets.get(MagicCardData.Rarity.MYTHIC_RARE).size());
        return true;
    }

    private static JsonObject writeFace(MagicCardData face) {
        JsonObject entry = new JsonObject();
        entry.addProperty("name", face.cardName());
        entry.addProperty("text", face.cardText());
        entry.addProperty("mana_cost", face.manaCost());
        entry.addProperty("type", face.typeLine());

        JsonArray colors = new JsonArray();
        face.colors().forEach(color -> colors.add(color.getSerializedName()));
        entry.add("colors", colors);

        JsonArray keywords = new JsonArray();
        face.keywords().forEach(keywords::add);
        entry.add("keywords", keywords);

        face.power().ifPresent(power -> entry.addProperty("power", power));
        face.toughness().ifPresent(toughness -> entry.addProperty("toughness", toughness));
        face.loyalty().ifPresent(loyalty -> entry.addProperty("loyalty", loyalty));
        face.defense().ifPresent(defense -> entry.addProperty("defense", defense));
        face.setId().ifPresent(setId -> entry.addProperty("set_id", setId.toString()));

        return entry;
    }
}
