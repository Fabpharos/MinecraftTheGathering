package com.fabpharos.minecraftthegathering.scryfall;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.fabpharos.minecraftthegathering.item.MagicCardData;
import com.fabpharos.minecraftthegathering.item.MagicCardFaces;

/**
 * Parses Scryfall card JSON objects into {@link MagicCardFaces}. The shape is the same whether the object
 * came from a live API response or a line of the bulk data JSONL dump.
 * <p>
 * A double-faced card (transform, modal DFC, etc.) carries a {@code card_faces} array with at least two
 * Card Face objects; when present, each face's own fields are used directly instead of the top-level ones
 * (which are often a merged/combined value, like a name of "Front // Back", not meant for gameplay use).
 */
public final class ScryfallCardParser {
    private ScryfallCardParser() {
    }

    /**
     * Parses {@code card} if it's a booster-legal paper card with a rarity this mod tracks
     * (common/uncommon/rare/mythic). Returns empty for digital-only cards, cards never sold in boosters,
     * or off-scale rarities like "special"/"bonus".
     */
    public static Optional<MagicCardFaces> parseBoosterLegalCard(JsonObject card) {
        if (!isBoosterLegalPaperCard(card)) {
            return Optional.empty();
        }

        MagicCardData.Rarity rarity = parseRarity(card);
        if (rarity == null) {
            return Optional.empty();
        }

        // Keywords and the set id are only ever given once for the whole card, not split per face.
        List<String> keywords = stringArrayField(card, "keywords");
        Optional<UUID> setId = stringOf(card, "set_id").flatMap(ScryfallCardParser::parseUuidOrEmpty);

        JsonArray faces = card.has("card_faces") && card.get("card_faces").isJsonArray() ? card.getAsJsonArray("card_faces") : null;
        if (faces != null && faces.size() >= 2) {
            MagicCardData front = parseFace(faces.get(0).getAsJsonObject(), rarity, keywords, setId);
            MagicCardData back = parseFace(faces.get(1).getAsJsonObject(), rarity, keywords, setId);
            return Optional.of(MagicCardFaces.revealed(front, back));
        }

        return Optional.of(MagicCardFaces.revealed(parseFace(card, rarity, keywords, setId), MagicCardData.BLANK));
    }

    private static boolean isBoosterLegalPaperCard(JsonObject card) {
        if (!card.has("booster") || !card.get("booster").getAsBoolean()) {
            return false;
        }

        if (!card.has("games") || !card.get("games").isJsonArray()) {
            return false;
        }

        for (JsonElement game : card.getAsJsonArray("games")) {
            if ("paper".equals(game.getAsString())) {
                return true;
            }
        }

        return false;
    }

    // Parses one face's own fields directly - `faceSource` is either the whole card (single-faced) or one
    // entry of its card_faces array (multi-faced). rarity/keywords/setId always come from the shared card
    // object, since Scryfall only ever gives one of each per card, not per face.
    private static MagicCardData parseFace(JsonObject faceSource, MagicCardData.Rarity rarity, List<String> keywords, Optional<UUID> setId) {
        String name = stringOf(faceSource, "name").orElse("Unknown Card");
        String text = stringOf(faceSource, "oracle_text").orElse("");
        String manaCost = stringOf(faceSource, "mana_cost").orElse("");
        String typeLine = stringOf(faceSource, "type_line").orElse("");

        return new MagicCardData(
                name,
                text,
                rarity,
                colorsOf(faceSource),
                manaCost,
                typeLine,
                intOf(faceSource, "power"),
                intOf(faceSource, "toughness"),
                intOf(faceSource, "loyalty"),
                intOf(faceSource, "defense"),
                keywords,
                setId);
    }

    private static Optional<UUID> parseUuidOrEmpty(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static List<MagicCardData.Color> colorsOf(JsonObject source) {
        if (!source.has("colors") || !source.get("colors").isJsonArray()) {
            return List.of();
        }

        List<MagicCardData.Color> colors = new ArrayList<>();
        for (JsonElement element : source.getAsJsonArray("colors")) {
            MagicCardData.Color color = MagicCardData.Color.fromManaLetter(element.getAsString());
            if (color != null) {
                colors.add(color);
            }
        }

        return colors;
    }

    private static List<String> stringArrayField(JsonObject card, String key) {
        if (!card.has(key) || !card.get(key).isJsonArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonElement element : card.getAsJsonArray(key)) {
            values.add(element.getAsString());
        }

        return values;
    }

    private static Optional<String> stringOf(JsonObject source, String key) {
        if (!source.has(key) || source.get(key).isJsonNull()) {
            return Optional.empty();
        }

        return Optional.of(source.get(key).getAsString());
    }

    // power/toughness/loyalty/defense can be "*", "1+*", "X", etc. for variable values - skip those rather
    // than storing a meaningless number.
    private static Optional<Integer> intOf(JsonObject source, String key) {
        return stringOf(source, key).flatMap(raw -> {
            try {
                return Optional.of(Integer.parseInt(raw.trim()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    // Scryfall's own rarity vocabulary ("mythic", "special", "bonus", ...) isn't the same as this mod's
    // persisted Rarity names (e.g. MYTHIC_RARE serializes as "mythic_rare") - map it explicitly rather
    // than comparing against getSerializedName(), which would silently drop every mythic card.
    @Nullable
    private static MagicCardData.Rarity parseRarity(JsonObject card) {
        if (!card.has("rarity") || card.get("rarity").isJsonNull()) {
            return null;
        }

        return switch (card.get("rarity").getAsString()) {
            case "common" -> MagicCardData.Rarity.COMMON;
            case "uncommon" -> MagicCardData.Rarity.UNCOMMON;
            case "rare" -> MagicCardData.Rarity.RARE;
            case "mythic" -> MagicCardData.Rarity.MYTHIC_RARE;
            default -> null;
        };
    }
}
