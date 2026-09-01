package com.fabpharos.minecraftthegathering.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The data a Magic Card item stack stores: its front face, its back face (real for a double-faced card,
 * {@link MagicCardData#BLANK} otherwise), and which of the two is currently being shown.
 */
public record MagicCardFaces(MagicCardData front, MagicCardData back, boolean showingBack) {
    public static final Codec<MagicCardFaces> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MagicCardData.CODEC.fieldOf("front").forGetter(MagicCardFaces::front),
            MagicCardData.CODEC.fieldOf("back").forGetter(MagicCardFaces::back),
            Codec.BOOL.fieldOf("showing_back").forGetter(MagicCardFaces::showingBack)
    ).apply(instance, MagicCardFaces::new));

    public static MagicCardFaces revealed(MagicCardData front, MagicCardData back) {
        return new MagicCardFaces(front, back, false);
    }

    public MagicCardData currentFace() {
        return showingBack ? back : front;
    }

    public boolean isDoubleFaced() {
        return !back.isBlank();
    }

    public MagicCardFaces toggled() {
        return new MagicCardFaces(front, back, !showingBack);
    }
}
