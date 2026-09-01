package com.fabpharos.minecraftthegathering.scryfall;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;
import com.fabpharos.minecraftthegathering.item.MagicCardData;
import com.fabpharos.minecraftthegathering.item.MagicCardFaces;

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
    private static volatile Map<MagicCardData.Rarity, List<MagicCardFaces>> buckets = Map.of();
    private static volatile List<MagicCardFaces> all = List.of();

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

        Map<MagicCardData.Rarity, List<MagicCardFaces>> newBuckets = new EnumMap<>(MagicCardData.Rarity.class);
        List<MagicCardFaces> newAll = new ArrayList<>();

        for (MagicCardData.Rarity rarity : MagicCardData.Rarity.values()) {
            List<MagicCardFaces> list = new ArrayList<>();
            if (root.has(rarity.getSerializedName())) {
                for (JsonElement element : root.getAsJsonArray(rarity.getSerializedName())) {
                    list.add(readCard(element.getAsJsonObject(), rarity));
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

    private static MagicCardFaces readCard(JsonObject entry, MagicCardData.Rarity rarity) {
        MagicCardData front = readFace(entry.getAsJsonObject("front"), rarity);
        MagicCardData back = entry.has("back") ? readFace(entry.getAsJsonObject("back"), rarity) : MagicCardData.BLANK;
        return MagicCardFaces.revealed(front, back);
    }

    private static MagicCardData readFace(JsonObject entry, MagicCardData.Rarity rarity) {
        List<MagicCardData.Color> colors = new ArrayList<>();
        if (entry.has("colors")) {
            for (JsonElement colorElement : entry.getAsJsonArray("colors")) {
                String serializedName = colorElement.getAsString();
                for (MagicCardData.Color color : MagicCardData.Color.values()) {
                    if (color.getSerializedName().equals(serializedName)) {
                        colors.add(color);
                        break;
                    }
                }
            }
        }

        List<String> keywords = new ArrayList<>();
        if (entry.has("keywords")) {
            for (JsonElement keywordElement : entry.getAsJsonArray("keywords")) {
                keywords.add(keywordElement.getAsString());
            }
        }

        return new MagicCardData(
                entry.get("name").getAsString(),
                entry.get("text").getAsString(),
                rarity,
                colors,
                entry.has("mana_cost") ? entry.get("mana_cost").getAsString() : "",
                entry.has("type") ? entry.get("type").getAsString() : "",
                entry.has("power") ? Optional.of(entry.get("power").getAsInt()) : Optional.empty(),
                entry.has("toughness") ? Optional.of(entry.get("toughness").getAsInt()) : Optional.empty(),
                entry.has("loyalty") ? Optional.of(entry.get("loyalty").getAsInt()) : Optional.empty(),
                entry.has("defense") ? Optional.of(entry.get("defense").getAsInt()) : Optional.empty(),
                keywords,
                entry.has("set_id") ? Optional.of(UUID.fromString(entry.get("set_id").getAsString())) : Optional.empty());
    }

    /** A uniformly random card from the whole pool (all rarities), matching their true relative frequency. */
    public static Optional<MagicCardFaces> randomCard() {
        List<MagicCardFaces> pool = all;
        return pool.isEmpty() ? Optional.empty() : Optional.of(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
    }

    /** A uniformly random card of exactly the given rarity. */
    public static Optional<MagicCardFaces> randomCard(MagicCardData.Rarity rarity) {
        List<MagicCardFaces> pool = buckets.getOrDefault(rarity, List.of());
        return pool.isEmpty() ? Optional.empty() : Optional.of(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
    }

    /**
     * A uniformly random card of exactly the given rarity, restricted to a specific set. An empty
     * {@code setId} means no restriction (same as {@link #randomCard(MagicCardData.Rarity)}).
     */
    public static Optional<MagicCardFaces> randomCard(MagicCardData.Rarity rarity, Optional<UUID> setId) {
        if (setId.isEmpty()) {
            return randomCard(rarity);
        }

        List<MagicCardFaces> matches = buckets.getOrDefault(rarity, List.of()).stream()
                .filter(card -> card.front().setId().equals(setId))
                .toList();
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(ThreadLocalRandom.current().nextInt(matches.size())));
    }

    /** A uniformly random card whose front face's type line contains {@code typeKeyword} (e.g. "creature"), case-insensitive. */
    public static Optional<MagicCardFaces> randomCardOfType(String typeKeyword) {
        String needle = typeKeyword.toLowerCase(Locale.ROOT);
        List<MagicCardFaces> matches = all.stream()
                .filter(card -> card.front().typeLine().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(ThreadLocalRandom.current().nextInt(matches.size())));
    }

    /**
     * The single best fuzzy match for {@code query} against every card's front-face name in the pool, or
     * empty if the pool holds nothing that even loosely resembles it. Exact/prefix/substring matches
     * always win over a fuzzy (subsequence) match; ties otherwise go to whichever card is encountered first.
     */
    public static Optional<MagicCardFaces> findByName(String query) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        MagicCardFaces best = null;
        int bestScore = -1;

        for (MagicCardFaces card : all) {
            int score = FuzzyMatch.score(normalizedQuery, card.front().cardName().toLowerCase(Locale.ROOT));
            if (score > bestScore) {
                bestScore = score;
                best = card;
            }
        }

        return Optional.ofNullable(best);
    }
}
