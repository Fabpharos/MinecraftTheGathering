package com.fabpharos.minecraftthegathering.inventory;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CardShredderMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SLOTS = 9;

    private final Container container;

    // Client-side: built with a blank container; the server pushes the real slot contents right after opening.
    public CardShredderMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SLOTS));
    }

    // Server-side: backed by the block entity itself.
    public CardShredderMenu(int containerId, Inventory playerInventory, Container container) {
        super(MinecraftTheGathering.CARD_SHREDDER_MENU.get(), containerId);
        checkContainerSize(container, CONTAINER_SLOTS);
        this.container = container;
        this.container.startOpen(playerInventory.player);

        // Card slot (row 0), scrap slots (row 1), wildcard slots (row 2) - see card_shredder_gui.png.
        this.addSlot(new RestrictedSlot(container, 0, 8, 18));
        for (int col = 0; col < 4; col++) {
            this.addSlot(new RestrictedSlot(container, 1 + col, 8 + col * 18, 36));
        }
        for (int col = 0; col < 4; col++) {
            this.addSlot(new RestrictedSlot(container, 5 + col, 8 + col * 18, 54));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 143));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();
            if (index < CONTAINER_SLOTS) {
                if (!this.moveItemStackTo(slotStack, CONTAINER_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, CONTAINER_SLOTS, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    // A slot that only accepts what the backing container's own canPlaceItem allows for that slot index -
    // the exact same rule hoppers are held to (see CardShredderBlockEntity#canPlaceItem).
    private static class RestrictedSlot extends Slot {
        RestrictedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.getContainerSlot(), stack);
        }
    }
}
