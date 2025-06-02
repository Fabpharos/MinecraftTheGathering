package com.fabpharos.minecraftthegathering.item;

import com.fabpharos.minecraftthegathering.Registration;
import com.fabpharos.minecraftthegathering.util.RandomCardFetcherThread;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class CardItem extends Item {

    public CardItem() {
        super(new Item.Properties().component(Registration.CARD_ITEM_COMPONENTS.get(), CardItemComponent.BLANK));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(!level.isClientSide) {
            ItemStack itemInHand = player.getItemInHand(usedHand);
            if(Objects.equals(itemInHand.getComponents().getOrDefault(Registration.CARD_ITEM_COMPONENTS.get(), CardItemComponent.BLANK).getName(), "Blank")) {
                (new RandomCardFetcherThread(itemInHand)).start();
                return InteractionResultHolder.success(itemInHand);
            }
        }
        return super.use(level, player, usedHand);
    }
}
