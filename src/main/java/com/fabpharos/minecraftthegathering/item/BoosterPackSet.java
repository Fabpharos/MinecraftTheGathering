package com.fabpharos.minecraftthegathering.item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The (optional) Magic set a Booster Pack is themed to. A pack with no set assigned draws from every card
 * in the local pool, same as before this existed; one with a set only draws cards printed in that set.
 */
public record BoosterPackSet(Optional<String> code, Optional<String> name, Optional<UUID> id) {
    public static final BoosterPackSet NONE = new BoosterPackSet(Optional.empty(), Optional.empty(), Optional.empty());

    public static final Codec<BoosterPackSet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("code").forGetter(BoosterPackSet::code),
            Codec.STRING.optionalFieldOf("name").forGetter(BoosterPackSet::name),
            UUIDUtil.STRING_CODEC.optionalFieldOf("id").forGetter(BoosterPackSet::id)
    ).apply(instance, BoosterPackSet::new));

    /** The set assigned to {@code stack}, or {@link #NONE} if it has none (the default for every pack). */
    public static BoosterPackSet of(ItemStack stack) {
        return stack.getOrDefault(MinecraftTheGathering.BOOSTER_PACK_SET.get(), NONE);
    }

    // Shows the set's name in the tooltip, if it has one. The pack's own item name is never changed by this.
    public void appendTooltip(List<Component> tooltip) {
        name.ifPresent(setName -> tooltip.add(Component.literal(setName)));
    }
}
