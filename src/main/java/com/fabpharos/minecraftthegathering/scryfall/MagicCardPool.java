package com.fabpharos.minecraftthegathering.scryfall;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;
import com.fabpharos.minecraftthegathering.item.MagicCardData;

/**
 * The local pool of Magic Cards, loaded once from the compact file {@link CardPoolBuilder} produces and
 * kept fully in memory so picking a random card (by rarity, or from the whole pool) is O(1) and needs
 * no network access.
 * <p>
 * Loaded on a background thread at server start, then read from freely on any thread afterwards -
 * the backing lists are replaced atomically (via a volatile reference to an immutable snapshot), never
 * mutated in place.
 */
public final class MagicCardPool {
    private static volatile Map<MagicCardData.Rarity, List<MagicCardData>> buckets = Map.of();
    private static volatile List<MagicCardData> all = List.of();

    private MagicCardPool() {
    }

    public static boolean isLoaded() {
        return !all.isEmpty();
    }

    /** Blocking - reads and parses the pool file. Call only from a background thread. */
    public static boolean load(Path poolFile) {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(poolFile, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            MinecraftTheGathering.LOGGER.warn("Failed to load the local Magic Card pool from {}", poolFile, e);
            return false;
        }

        Map<MagicCardData.Rarity, List<MagicCardData>> newBuckets = new EnumMap<>(MagicCardData.Rarity.class);
        List<MagicCardData> newAll = new ArrayList<>();

        for (MagicCardData.Rarity rarity : MagicCardData.Rarity.values()) {
            List<MagicCardData> list = new ArrayList<>();
            if (root.has(rarity.getSerializedName())) {
                for (JsonElement element : root.getAsJsonArray(rarity.getSerializedName())) {
                    JsonObject entry = element.getAsJsonObject();
                    String name = entry.get("name").getAsString();
                    String text = entry.get("text").getAsString();
                    list.add(new MagicCardData(name, text, rarity));
                }
            }

            newBuckets.put(rarity, List.copyOf(list));
            newAll.addAll(list);
        }

        buckets = Map.copyOf(newBuckets);
        all = List.copyOf(newAll);

        MinecraftTheGathering.LOGGER.info(
                "Loaded {} Magic Cards into the local card pool ({} common, {} uncommon, {} rare, {} mythic rare)",
                all.size(),
                buckets.get(MagicCardData.Rarity.COMMON).size(),
                buckets.get(MagicCardData.Rarity.UNCOMMON).size(),
                buckets.get(MagicCardData.Rarity.RARE).size(),
                buckets.get(MagicCardData.Rarity.MYTHIC_RARE).size());
        return isLoaded();
    }

    /** A uniformly random card from the whole pool (all rarities), matching their true relative frequency. */
    public static Optional<MagicCardData> randomCard() {
        List<MagicCardData> pool = all;
        return pool.isEmpty() ? Optional.empty() : Optional.of(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
    }

    /** A uniformly random card of exactly the given rarity. */
    public static Optional<MagicCardData> randomCard(MagicCardData.Rarity rarity) {
        List<MagicCardData> pool = buckets.getOrDefault(rarity, List.of());
        return pool.isEmpty() ? Optional.empty() : Optional.of(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
    }
}
