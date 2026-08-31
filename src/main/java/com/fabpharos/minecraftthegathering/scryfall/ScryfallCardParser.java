package com.fabpharos.minecraftthegathering.scryfall;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.fabpharos.minecraftthegathering.item.MagicCardData;

/**
 * Parses Scryfall card JSON objects into {@link MagicCardData}. The shape is the same whether the object
 * came from a live API response or a line of the bulk data JSONL dump.
 */
public final class ScryfallCardParser {
    private ScryfallCardParser() {
    }

    /**
     * Parses {@code card} if it's a booster-legal paper card with a rarity this mod tracks
     * (common/uncommon/rare/mythic). Returns empty for digital-only cards, cards never sold in boosters,
     * or off-scale rarities like "special"/"bonus".
     */
    public static Optional<MagicCardData> parseBoosterLegalCard(JsonObject card) {
        if (!isBoosterLegalPaperCard(card)) {
            return Optional.empty();
        }

        MagicCardData.Rarity rarity = parseRarity(card);
        if (rarity == null) {
            return Optional.empty();
        }

        String name = card.has("name") && !card.get("name").isJsonNull() ? card.get("name").getAsString() : "Unknown Card";
        return Optional.of(new MagicCardData(name, extractOracleText(card), rarity));
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

    // Single-faced cards carry oracle_text at the top level; double-faced cards split it across card_faces.
    private static String extractOracleText(JsonObject card) {
        if (card.has("oracle_text") && !card.get("oracle_text").isJsonNull()) {
            return card.get("oracle_text").getAsString();
        }

        if (card.has("card_faces") && card.get("card_faces").isJsonArray()) {
            List<String> faceTexts = new ArrayList<>();
            for (JsonElement faceElement : card.getAsJsonArray("card_faces")) {
                JsonObject face = faceElement.getAsJsonObject();
                if (face.has("oracle_text") && !face.get("oracle_text").isJsonNull()) {
                    faceTexts.add(face.get("oracle_text").getAsString());
                }
            }

            if (!faceTexts.isEmpty()) {
                return String.join("\n\n", faceTexts);
            }
        }

        return "";
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
