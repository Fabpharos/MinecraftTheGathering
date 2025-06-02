package com.fabpharos.minecraftthegathering.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.List;
import java.util.function.Consumer;

public class BoosterPackItemComponent implements TooltipProvider {

    private final String setCode;
    private final String setName;
    private final List<ItemStack> contents;

    private final boolean opened;

    public BoosterPackItemComponent(String setCode, String setName, List<ItemStack> contents, boolean opened) {
        this.setCode = setCode;
        this.setName = setName;
        this.contents = contents;
        this.opened = opened;
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag) {
        consumer.accept(Component.literal(setName + " (" + setName + ")"));
        if(opened)
            consumer.accept(Component.literal("opened."));
        if(contents != null && !contents.isEmpty())
            consumer.accept(Component.literal("Contains " + contents.size() + " cards."));

    }

    public String getSetCode() {
        return setCode;
    }

    public String getSetName() {
        return setName;
    }

    public List<ItemStack> getContents() {
        return contents;
    }

    public boolean isOpened() {
        return opened;
    }
}
