package com.fabpharos.minecraftthegathering.scryfall;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;

/**
 * The local pool of Magic sets (core/expansion/masters/eternal only), loaded once from the file
 * {@link ScryfallSetService} produces and kept fully in memory - mirrors {@link MagicCardPool}.
 */
public final class MagicSetPool {
    private static volatile List<MagicSet> all = List.of();
    private static volatile Map<String, MagicSet> byCode = Map.of();
    private static volatile Map<UUID, MagicSet> byId = Map.of();

    private MagicSetPool() {
    }

    public static boolean isLoaded() {
        return !all.isEmpty();
    }

    /** Blocking - reads and parses the sets file. Call only from a background thread. */
    public static boolean load(Path setsFile) {
        JsonArray root;
        try (Reader reader = Files.newBufferedReader(setsFile, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonArray();
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            MinecraftTheGathering.LOGGER.warn("Failed to load the local Magic set pool from {}", setsFile, e);
            return false;
        }

        List<MagicSet> newAll = new ArrayList<>();
        for (var element : root) {
            JsonObject entry = element.getAsJsonObject();
            newAll.add(new MagicSet(
                    UUID.fromString(entry.get("id").getAsString()),
                    entry.get("code").getAsString(),
                    entry.get("name").getAsString(),
                    entry.get("set_type").getAsString()));
        }

        all = List.copyOf(newAll);
        byCode = newAll.stream().collect(Collectors.toUnmodifiableMap(MagicSet::code, set -> set, (a, b) -> a));
        byId = newAll.stream().collect(Collectors.toUnmodifiableMap(MagicSet::id, set -> set, (a, b) -> a));

        MinecraftTheGathering.LOGGER.info("Loaded {} Magic sets into the local set pool", all.size());
        return isLoaded();
    }

    public static List<MagicSet> all() {
        return all;
    }

    public static Optional<MagicSet> byCode(String code) {
        return Optional.ofNullable(byCode.get(code.toLowerCase(Locale.ROOT)));
    }

    public static Optional<MagicSet> byId(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Resolves {@code query} against every set's code and name: an exact (case-insensitive) code match
     * always wins; otherwise, the single best fuzzy match against set names, or empty if nothing even
     * loosely resembles it.
     */
    public static Optional<MagicSet> find(String query) {
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);

        Optional<MagicSet> exactCode = byCode(normalizedQuery);
        if (exactCode.isPresent()) {
            return exactCode;
        }

        MagicSet best = null;
        int bestScore = -1;
        for (MagicSet set : all) {
            int score = FuzzyMatch.score(normalizedQuery, set.name().toLowerCase(Locale.ROOT));
            if (score > bestScore) {
                bestScore = score;
                best = set;
            }
        }

        return Optional.ofNullable(best);
    }

    public record MagicSet(UUID id, String code, String name, String setType) {
    }
}
