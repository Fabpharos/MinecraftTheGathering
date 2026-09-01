package com.fabpharos.minecraftthegathering.item;

import java.util.List;

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

public class OpenedBoosterPackItem extends Item {
    public OpenedBoosterPackItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        BoosterPackSet.of(stack).appendTooltip(tooltipComponents);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new BoosterPackMenu(containerId, inventory, stack),
                    Component.translatable("container.minecraftthegathering.booster_pack")));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
