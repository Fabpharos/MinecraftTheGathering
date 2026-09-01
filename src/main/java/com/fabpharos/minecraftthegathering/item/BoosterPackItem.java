package com.fabpharos.minecraftthegathering.item;

import java.util.List;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;
import com.fabpharos.minecraftthegathering.inventory.BoosterPackMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class BoosterPackItem extends Item {
    public BoosterPackItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        BoosterPackSet.of(stack).appendTooltip(tooltipComponents);
    }

    // Breaks the seal: swaps itself for an Opened Booster Pack (carrying its set assignment, if any) and
    // opens its GUI, which fills the pack with cards.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ItemStack openedStack = new ItemStack(MinecraftTheGathering.OPENED_BOOSTER_PACK_ITEM.get());
            openedStack.set(MinecraftTheGathering.BOOSTER_PACK_SET.get(), BoosterPackSet.of(stack));
            player.setItemInHand(hand, openedStack);
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new BoosterPackMenu(containerId, inventory, openedStack),
                    Component.translatable("container.minecraftthegathering.booster_pack")));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
