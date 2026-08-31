package com.fabpharos.minecraftthegathering.item;

import java.util.List;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;
import com.fabpharos.minecraftthegathering.scryfall.MagicCardPool;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class MagicCardItem extends Item {
    public MagicCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        MagicCardData data = stack.get(MinecraftTheGathering.MAGIC_CARD_DATA.get());
        return data != null ? Component.literal(data.cardName()) : super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        MagicCardData data = stack.get(MinecraftTheGathering.MAGIC_CARD_DATA.get());
        if (data == null) {
            tooltipComponents.add(Component.translatable("tooltip.minecraftthegathering.magic_card_item.no_data"));
            return;
        }

        // oracle_text commonly contains embedded newlines (multiple abilities); each needs its own tooltip line.
        for (String line : data.cardText().split("\n")) {
            tooltipComponents.add(Component.literal(line));
        }

        MutableComponent rarityText = Component.translatable("rarity.minecraftthegathering." + data.rarity().getSerializedName());
        if (data.rarity().color() != null) {
            rarityText.withStyle(data.rarity().color());
        }
        tooltipComponents.add(rarityText);
    }

    // A card with no data yet picks a random one from the local card pool on right click. If there's more
    // than one blank card in the stack, only one is revealed: it's split off into its own stack rather than
    // stamping the same random card onto the whole stack.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && stack.get(MinecraftTheGathering.MAGIC_CARD_DATA.get()) == null) {
            MagicCardPool.randomCard().ifPresentOrElse(
                    data -> reveal(player, stack, data),
                    () -> MinecraftTheGathering.LOGGER.warn("No local Magic Card data available yet; the card pool may still be loading."));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void reveal(Player player, ItemStack stack, MagicCardData data) {
        if (stack.getCount() > 1) {
            stack.shrink(1);
            ItemStack revealed = new ItemStack(this, 1);
            revealed.set(MinecraftTheGathering.MAGIC_CARD_DATA.get(), data);
            if (!player.getInventory().add(revealed)) {
                player.drop(revealed, true);
            }
        } else {
            stack.set(MinecraftTheGathering.MAGIC_CARD_DATA.get(), data);
        }
    }
}
