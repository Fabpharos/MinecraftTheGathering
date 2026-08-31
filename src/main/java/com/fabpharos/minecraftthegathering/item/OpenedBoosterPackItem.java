package com.fabpharos.minecraftthegathering.item;

import com.fabpharos.minecraftthegathering.inventory.BoosterPackMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class OpenedBoosterPackItem extends Item {
    public OpenedBoosterPackItem(Properties properties) {
        super(properties);
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
