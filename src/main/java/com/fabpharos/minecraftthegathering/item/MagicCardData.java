package com.fabpharos.minecraftthegathering.item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.StringRepresentable;

public record MagicCardData(
        String cardName,
        String cardText,
        MagicCardData.Rarity rarity,
        List<MagicCardData.Color> colors,
        String manaCost,
        String typeLine,
        Optional<Integer> power,
        Optional<Integer> toughness,
        Optional<Integer> loyalty,
        Optional<Integer> defense,
        List<String> keywords,
        Optional<UUID> setId) {
    // Represents "no face here" - the back of a single-faced card, standing in for a real MagicCardData
    // so a card doesn't need a second, nullable field just to say it has no back face.
    public static final MagicCardData BLANK = new MagicCardData(
            "", "", Rarity.COMMON, List.of(), "", "",
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty());

    public static final Codec<MagicCardData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("card_name").forGetter(MagicCardData::cardName),
            Codec.STRING.fieldOf("card_text").forGetter(MagicCardData::cardText),
            Rarity.CODEC.fieldOf("rarity").forGetter(MagicCardData::rarity),
            Color.CODEC.listOf().fieldOf("colors").forGetter(MagicCardData::colors),
            Codec.STRING.fieldOf("mana_cost").forGetter(MagicCardData::manaCost),
            Codec.STRING.fieldOf("type").forGetter(MagicCardData::typeLine),
            Codec.INT.optionalFieldOf("power").forGetter(MagicCardData::power),
            Codec.INT.optionalFieldOf("toughness").forGetter(MagicCardData::toughness),
            Codec.INT.optionalFieldOf("loyalty").forGetter(MagicCardData::loyalty),
            Codec.INT.optionalFieldOf("defense").forGetter(MagicCardData::defense),
            Codec.STRING.listOf().fieldOf("keywords").forGetter(MagicCardData::keywords),
            UUIDUtil.STRING_CODEC.optionalFieldOf("set_id").forGetter(MagicCardData::setId)
    ).apply(instance, MagicCardData::new));

    // Frame tints for the card-art texture layer, matching Magic's own card-frame convention.
    private static final int COLORLESS_RGB = 0xB0B0B0;
    private static final int MULTICOLOR_RGB = 0xD4AF37;

    public boolean isBlank() {
        return cardName.isEmpty();
    }

    // The frame tint for this card's color identity: grey if colorless, that color's own RGB if
    // monocolored, gold if multicolored.
    public int frameColorRgb() {
        return switch (colors.size()) {
            case 0 -> COLORLESS_RGB;
            case 1 -> colors.get(0).rgb();
            default -> MULTICOLOR_RGB;
        };
    }

    public enum Rarity implements StringRepresentable {
        COMMON("common", null),
        UNCOMMON("uncommon", ChatFormatting.GRAY),
        RARE("rare", ChatFormatting.YELLOW),
        MYTHIC_RARE("mythic_rare", ChatFormatting.GOLD);

        public static final Codec<Rarity> CODEC = StringRepresentable.fromEnum(Rarity::values);

        private final String serializedName;
        private final ChatFormatting color;

        Rarity(String serializedName, ChatFormatting color) {
            this.serializedName = serializedName;
            this.color = color;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        // The color used to represent this rarity in the UI. Null means no special color (Common).
        @Nullable
        public ChatFormatting color() {
            return color;
        }
    }

    // WUBRG. A card can have zero of these (colorless) up to all five.
    public enum Color implements StringRepresentable {
        WHITE("white", 0xFFFBD5),
        BLUE("blue", 0x4FA8FF),
        BLACK("black", 0xB6AFC7),
        RED("red", 0xFF5C5C),
        GREEN("green", 0x3ECC5F);

        public static final Codec<Color> CODEC = StringRepresentable.fromEnum(Color::values);

        private final String serializedName;
        private final int rgb;

        Color(String serializedName, int rgb) {
            this.serializedName = serializedName;
            this.rgb = rgb;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public int rgb() {
            return rgb;
        }

        // Maps a single-letter mana symbol ("W", "u", ...) to its color, or null if it isn't a plain WUBRG symbol.
        @Nullable
        public static Color fromManaLetter(String letter) {
            if (letter.length() != 1) {
                return null;
            }

            return switch (Character.toUpperCase(letter.charAt(0))) {
                case 'W' -> WHITE;
                case 'U' -> BLUE;
                case 'B' -> BLACK;
                case 'R' -> RED;
                case 'G' -> GREEN;
                default -> null;
            };
        }
    }
}
