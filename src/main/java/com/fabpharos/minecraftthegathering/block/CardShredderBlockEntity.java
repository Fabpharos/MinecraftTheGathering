package com.fabpharos.minecraftthegathering.block;

import javax.annotation.Nullable;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;
import com.fabpharos.minecraftthegathering.inventory.CardShredderMenu;
import com.fabpharos.minecraftthegathering.item.MagicCardData;
import com.fabpharos.minecraftthegathering.item.MagicCardFaces;
import com.fabpharos.minecraftthegathering.item.MagicCardItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * A 9-slot machine: 1 card-input slot on top, 4 rarity-specific scrap slots (sides), and 4 rarity-specific
 * wildcard output slots (bottom). Every server tick, it shreds one unit off the card in its input slot into
 * scrap of the matching rarity; if there's no (shreddable) card there, it instead upgrades any of its four
 * scrap slots that hold 8 or more into one wildcard of that rarity - all four are checked independently, so
 * more than one can convert in the same tick.
 */
public class CardShredderBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int CARD_SLOT = 0;
    public static final int CONTAINER_SIZE = 9;
    private static final int SCRAP_SLOTS_START = 1;
    private static final int WILDCARD_SLOTS_START = 5;

    private static final int[] SLOTS_UP = {CARD_SLOT};
    private static final int[] SLOTS_DOWN = {5, 6, 7, 8};
    private static final int[] SLOTS_SIDE = {1, 2, 3, 4};

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public CardShredderBlockEntity(BlockPos pos, BlockState state) {
        super(MinecraftTheGathering.CARD_SHREDDER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CardShredderBlockEntity entity) {
        if (!entity.tryShredCard()) {
            entity.tryUpgradeAllScrap();
        }
    }

    // Shreds one unit of the input card into scrap of its rarity. Returns false (and shreds nothing) if
    // there's no card, the card has no revealed data to determine a rarity from, or the destination scrap
    // slot has no room.
    private boolean tryShredCard() {
        ItemStack card = this.items.get(CARD_SLOT);
        if (card.isEmpty()) {
            return false;
        }

        MagicCardFaces faces = card.get(MinecraftTheGathering.MAGIC_CARD_DATA.get());
        if (faces == null || faces.currentFace().isBlank()) {
            return false;
        }

        MagicCardData.Rarity rarity = faces.currentFace().rarity();
        int scrapSlot = scrapSlot(rarity);
        ItemStack scrapStack = this.items.get(scrapSlot);
        Item scrapItem = scrapItemFor(rarity).get();
        if (!scrapStack.isEmpty() && (!scrapStack.is(scrapItem) || scrapStack.getCount() >= scrapStack.getMaxStackSize())) {
            return false;
        }

        card.shrink(1);
        if (scrapStack.isEmpty()) {
            this.items.set(scrapSlot, new ItemStack(scrapItem));
        } else {
            scrapStack.grow(1);
        }

        this.setChanged();
        return true;
    }

    // Independently upgrades each of the 4 scrap slots that has 8+ scrap and room in its wildcard slot.
    private void tryUpgradeAllScrap() {
        for (MagicCardData.Rarity rarity : MagicCardData.Rarity.values()) {
            ItemStack scrapStack = this.items.get(scrapSlot(rarity));
            if (scrapStack.getCount() < 8) {
                continue;
            }

            int wildcardSlot = wildcardSlot(rarity);
            ItemStack wildcardStack = this.items.get(wildcardSlot);
            Item wildcardItem = wildcardItemFor(rarity).get();
            if (!wildcardStack.isEmpty() && (!wildcardStack.is(wildcardItem) || wildcardStack.getCount() >= wildcardStack.getMaxStackSize())) {
                continue;
            }

            scrapStack.shrink(8);
            if (wildcardStack.isEmpty()) {
                this.items.set(wildcardSlot, new ItemStack(wildcardItem));
            } else {
                wildcardStack.grow(1);
            }

            this.setChanged();
        }
    }

    private static int scrapSlot(MagicCardData.Rarity rarity) {
        return SCRAP_SLOTS_START + rarity.ordinal();
    }

    private static int wildcardSlot(MagicCardData.Rarity rarity) {
        return WILDCARD_SLOTS_START + rarity.ordinal();
    }

    private static DeferredItem<Item> scrapItemFor(MagicCardData.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> MinecraftTheGathering.COMMON_SCRAP_ITEM;
            case UNCOMMON -> MinecraftTheGathering.UNCOMMON_SCRAP_ITEM;
            case RARE -> MinecraftTheGathering.RARE_SCRAP_ITEM;
            case MYTHIC_RARE -> MinecraftTheGathering.MYTHIC_RARE_SCRAP_ITEM;
        };
    }

    private static DeferredItem<Item> wildcardItemFor(MagicCardData.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> MinecraftTheGathering.COMMON_WILDCARD_ITEM;
            case UNCOMMON -> MinecraftTheGathering.UNCOMMON_WILDCARD_ITEM;
            case RARE -> MinecraftTheGathering.RARE_WILDCARD_ITEM;
            case MYTHIC_RARE -> MinecraftTheGathering.MYTHIC_RARE_WILDCARD_ITEM;
        };
    }

    // The top slot only takes Magic Cards; the next four only take their matching scrap; the last four
    // only take their matching wildcard. Applies to hopper insertion and manual GUI placement alike.
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == CARD_SLOT) {
            return stack.getItem() instanceof MagicCardItem;
        }
        if (slot >= SCRAP_SLOTS_START && slot < SCRAP_SLOTS_START + 4) {
            return stack.is(scrapItemFor(MagicCardData.Rarity.values()[slot - SCRAP_SLOTS_START]).get());
        }
        if (slot >= WILDCARD_SLOTS_START && slot < WILDCARD_SLOTS_START + 4) {
            return stack.is(wildcardItemFor(MagicCardData.Rarity.values()[slot - WILDCARD_SLOTS_START]).get());
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) {
            return SLOTS_UP;
        }
        return side == Direction.DOWN ? SLOTS_DOWN : SLOTS_SIDE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.minecraftthegathering.card_shredder");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CardShredderMenu(containerId, inventory, this);
    }
}
