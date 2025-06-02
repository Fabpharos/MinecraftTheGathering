package com.fabpharos.minecraftthegathering.item;

import com.google.gson.JsonArray;
import com.mojang.datafixers.util.Function10;
import com.mojang.datafixers.util.Function6;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class CardItemComponent implements TooltipProvider {

    public static HashMap<Character, Style> symbolTextFormatting = new HashMap<>();
    public static HashMap<String, Style> rarityTextFormatting = new HashMap<>();

    public static final CardItemComponent BLANK = new CardItemComponent("Blank", "", "", "", "", "", "", "", "", false);

    static {
        symbolTextFormatting.put('W', Style.EMPTY.withColor(ChatFormatting.YELLOW));
        symbolTextFormatting.put('U', Style.EMPTY.withColor(ChatFormatting.BLUE));
        symbolTextFormatting.put('B', Style.EMPTY.withColor(ChatFormatting.DARK_GRAY));
        symbolTextFormatting.put('R', Style.EMPTY.withColor(ChatFormatting.DARK_RED));
        symbolTextFormatting.put('G', Style.EMPTY.withColor(ChatFormatting.DARK_GREEN));
        symbolTextFormatting.put('C', Style.EMPTY.withColor(ChatFormatting.GRAY));
        symbolTextFormatting.put('S', Style.EMPTY.withColor(ChatFormatting.WHITE));

        rarityTextFormatting.put("common", Style.EMPTY.withColor(ChatFormatting.WHITE));
        rarityTextFormatting.put("uncommon", Style.EMPTY.withColor(ChatFormatting.GRAY));
        rarityTextFormatting.put("rare", Style.EMPTY.withColor(ChatFormatting.YELLOW));
        rarityTextFormatting.put("mythic", Style.EMPTY.withColor(ChatFormatting.GOLD));

    }

    private final String cardName;
    private final String manaCost;
    private final String typeLine;
    private final String cardText;

    private final String power;
    private final String toughness;
    private final String loyalty;
    private final String image;

    private final String color;
    private final boolean foil;

    public static final Codec<CardItemComponent> CODEC;
    public static final StreamCodec<RegistryFriendlyByteBuf, CardItemComponent> STREAM_CODEC;

    public CardItemComponent(String name, String manaCost, String typeLine, String cardText, String power, String toughness, String loyalty, String image, String color, boolean foil) {
        this.cardName = name;
        this.manaCost = manaCost;
        this.typeLine = typeLine;
        this.cardText = cardText;
        this.power = power;
        this.toughness = toughness;
        this.loyalty = loyalty;
        this.image = image;
        this.color = color;
        this.foil = foil;
    }

    static {
        CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("card_name").forGetter(CardItemComponent::getName),
                        Codec.STRING.fieldOf("mana_cost").forGetter(CardItemComponent::getManaCost),
                        Codec.STRING.fieldOf("type_line").forGetter(CardItemComponent::getTypeLine),
                        Codec.STRING.fieldOf("card_text").forGetter(CardItemComponent::getCardText),
                        Codec.STRING.fieldOf("power").forGetter(CardItemComponent::getPower),
                        Codec.STRING.fieldOf("toughness").forGetter(CardItemComponent::getToughness),
                        Codec.STRING.fieldOf("loyalty").forGetter(CardItemComponent::getLoyalty),
                        Codec.STRING.fieldOf("image").forGetter(CardItemComponent::getImage),
                        Codec.STRING.fieldOf("color").forGetter(CardItemComponent::getColor),
                        Codec.BOOL.fieldOf("foil").forGetter(CardItemComponent::isFoil)
                ).apply(instance, CardItemComponent::new)
        );
        STREAM_CODEC = composite(
                ByteBufCodecs.STRING_UTF8, CardItemComponent::getName,
                ByteBufCodecs.STRING_UTF8, CardItemComponent::getManaCost,
                ByteBufCodecs.STRING_UTF8, CardItemComponent::getTypeLine,
                ByteBufCodecs.STRING_UTF8, CardItemComponent::getCardText,
                ByteBufCodecs.STRING_UTF8, CardItemComponent::getPower,
                ByteBufCodecs.STRING_UTF8, CardItemComponent::getToughness,
                ByteBufCodecs.STRING_UTF8, CardItemComponent::getLoyalty,
                ByteBufCodecs.STRING_UTF8, CardItemComponent::getImage,
                ByteBufCodecs.STRING_UTF8, CardItemComponent::getColor,
                ByteBufCodecs.BOOL, CardItemComponent::isFoil,
                CardItemComponent::new
        );
    }

    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2,
            final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3,
            final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4,
            final StreamCodec<? super B, T5> codec5, final Function<C, T5> getter5,
            final StreamCodec<? super B, T6> codec6, final Function<C, T6> getter6,
            final StreamCodec<? super B, T7> codec7, final Function<C, T7> getter7,
            final StreamCodec<? super B, T8> codec8, final Function<C, T8> getter8,
            final StreamCodec<? super B, T9> codec9, final Function<C, T9> getter9,
            final StreamCodec<? super B, T10> codec10, final Function<C, T10> getter10,
            final Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, C> factory) {
        return new StreamCodec<B, C>() {
            public C decode(B p_330310_) {
                T1 t1 = (T1)codec1.decode(p_330310_);
                T2 t2 = (T2)codec2.decode(p_330310_);
                T3 t3 = (T3)codec3.decode(p_330310_);
                T4 t4 = (T4)codec4.decode(p_330310_);
                T5 t5 = (T5)codec5.decode(p_330310_);
                T6 t6 = (T6)codec6.decode(p_330310_);
                T7 t7 = (T7)codec7.decode(p_330310_);
                T8 t8 = (T8)codec8.decode(p_330310_);
                T9 t9 = (T9)codec9.decode(p_330310_);
                T10 t10 = (T10)codec10.decode(p_330310_);
                return (C)factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10);
            }

            public void encode(B p_332052_, C p_331912_) {
                codec1.encode(p_332052_, getter1.apply(p_331912_));
                codec2.encode(p_332052_, getter2.apply(p_331912_));
                codec3.encode(p_332052_, getter3.apply(p_331912_));
                codec4.encode(p_332052_, getter4.apply(p_331912_));
                codec5.encode(p_332052_, getter5.apply(p_331912_));
                codec6.encode(p_332052_, getter6.apply(p_331912_));
                codec7.encode(p_332052_, getter7.apply(p_331912_));
                codec8.encode(p_332052_, getter8.apply(p_331912_));
                codec9.encode(p_332052_, getter9.apply(p_331912_));
                codec10.encode(p_332052_, getter10.apply(p_331912_));
            }
        };
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag flag) {
        consumer.accept(Component.literal(cardName));
        if(flag.hasShiftDown()) {
            consumer.accept(Component.literal(cardText));
            if(!power.isBlank() && !toughness.isBlank())
                consumer.accept(Component.literal(power+"/"+toughness));
        } else {
            consumer.accept(Component.translatable("tooltip.minecraftthegathering.card_item_message"));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else {
            boolean b = false;
            if(obj instanceof CardItemComponent itemComponent) {
                b = Objects.equals(cardName, itemComponent.cardName)
                        && Objects.equals(manaCost, itemComponent.manaCost)
                        && Objects.equals(typeLine, itemComponent.typeLine)
                        && Objects.equals(cardText, itemComponent.cardText)
                        && Objects.equals(power, itemComponent.power)
                        && Objects.equals(toughness, itemComponent.toughness)
                        && Objects.equals(loyalty, itemComponent.loyalty)
                        && Objects.equals(image, itemComponent.image)
                        && Objects.equals(color, itemComponent.color)
                        && Objects.equals(foil, itemComponent.foil);
            }
            return b;
        }
    }

    @Override
    public int hashCode() {
        int i = this.cardName.hashCode();
        i = 31*i + this.manaCost.hashCode();
        i = 31*i + this.typeLine.hashCode();
        i = 31*i + this.cardText.hashCode();
        i = 31*i + this.power.hashCode();
        i = 31*i + this.toughness.hashCode();
        i = 31*i + this.loyalty.hashCode();
        i = 31*i + this.image.hashCode();
        i = 31*i + this.color.hashCode();
        i = 31*i + (this.foil ? 1:0);
        return i;
    }

    public String getName() {
        return cardName;
    }

    public String getCardText() {
        return cardText;
    }

    public String getPower() {
        return power;
    }

    public String getToughness() {
        return toughness;
    }

    public boolean isFoil() {
        return foil;
    }

    public String getManaCost() {
        return manaCost;
    }

    public String getTypeLine() {
        return typeLine;
    }

    public String getLoyalty() {
        return loyalty;
    }

    public String getImage() {
        return image;
    }

    public String getColor() {
        return color;
    }
}
