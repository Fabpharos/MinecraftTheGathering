package com.fabpharos.minecraftthegathering.item;

import com.fabpharos.minecraftthegathering.Registration;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

public class BoosterPackItem extends Item {
    public BoosterPackItem() {
        super(new Item.Properties().component(
                Registration.BOOSTER_PACK_CONTENTS.get(),
                ItemContainerContents.fromItems(NonNullList.withSize(20, ItemStack.EMPTY))));

    }
}
