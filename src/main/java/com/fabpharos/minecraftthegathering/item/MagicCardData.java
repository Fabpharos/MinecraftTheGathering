package com.fabpharos.minecraftthegathering.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public record MagicCardData(String cardName, String cardText, MagicCardData.Rarity rarity) {
    public static final Codec<MagicCardData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("card_name").forGetter(MagicCardData::cardName),
            Codec.STRING.fieldOf("card_text").forGetter(MagicCardData::cardText),
            Rarity.CODEC.fieldOf("rarity").forGetter(MagicCardData::rarity)
    ).apply(instance, MagicCardData::new));

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
        public ChatFormatting color() {
            return color;
        }
    }
}
