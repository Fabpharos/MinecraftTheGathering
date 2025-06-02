package com.fabpharos.minecraftthegathering.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;

public class CardItem extends Item {
    public static HashMap<Character, Style> symbolTextFormatting = new HashMap<>();
    public static HashMap<String, Style> rarityTextFormatting = new HashMap<>();

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

    public CardItem() {
        super(new Item.Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {

    }


}
