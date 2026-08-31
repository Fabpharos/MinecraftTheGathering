package com.fabpharos.minecraftthegathering.item;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;
import com.fabpharos.minecraftthegathering.inventory.BoosterPackMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BoosterPackItem extends Item {
    public BoosterPackItem(Properties properties) {
        super(properties);
    }

    // Breaks the seal: swaps itself for an Opened Booster Pack and opens its GUI, which fills the pack with cards.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ItemStack openedStack = new ItemStack(MinecraftTheGathering.OPENED_BOOSTER_PACK_ITEM.get());
            player.setItemInHand(hand, openedStack);
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new BoosterPackMenu(containerId, inventory, openedStack),
                    Component.translatable("container.minecraftthegathering.booster_pack")));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
