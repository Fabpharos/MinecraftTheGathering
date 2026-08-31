package com.fabpharos.minecraftthegathering.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;
import com.fabpharos.minecraftthegathering.item.MagicCardData;
import com.fabpharos.minecraftthegathering.item.MagicCardItem;
import com.fabpharos.minecraftthegathering.scryfall.MagicCardPool;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public class BoosterPackMenu extends AbstractContainerMenu {
    private static final int CONTAINER_ROWS = 2;
    private static final int CONTAINER_SLOTS = CONTAINER_ROWS * 9;

    private final SimpleContainer packContainer;
    private final ItemStack packStack;

    // Client-side: built with a blank container; the server pushes the real slot contents right after opening.
    public BoosterPackMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ItemStack.EMPTY, new SimpleContainer(CONTAINER_SLOTS));
    }

    // Server-side: backed by the pack stack itself. Fills it with cards the first time it's opened.
    public BoosterPackMenu(int containerId, Inventory playerInventory, ItemStack packStack) {
        this(containerId, playerInventory, packStack, buildContainer(packStack));
    }

    private BoosterPackMenu(int containerId, Inventory playerInventory, ItemStack packStack, SimpleContainer packContainer) {
        super(MinecraftTheGathering.BOOSTER_PACK_MENU.get(), containerId);
        this.packStack = packStack;
        this.packContainer = packContainer;
        this.packContainer.startOpen(playerInventory.player);

        if (!packStack.isEmpty()) {
            this.packContainer.addListener(container -> packStack.set(
                    DataComponents.CONTAINER, ItemContainerContents.fromItems(packContainer.getItems())));
        }

        for (int row = 0; row < CONTAINER_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new MagicCardSlot(packContainer, col + row * 9, 10 + col * 18, 14 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 10 + col * 18, 86 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 10 + col * 18, 144));
        }
    }

    // Reads the pack's stored cards into a fresh container, generating them from the local card pool on first open:
    // 11 commons, 3 uncommons, and a final slot that's rare with a 1-in-6 chance of being mythic instead.
    private static SimpleContainer buildContainer(ItemStack packStack) {
        SimpleContainer container = new SimpleContainer(CONTAINER_SLOTS);
        if (!packStack.has(DataComponents.CONTAINER)) {
            List<ItemStack> cards = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                cards.add(randomCardStack(MagicCardData.Rarity.COMMON));
            }
            for (int i = 0; i < 3; i++) {
                cards.add(randomCardStack(MagicCardData.Rarity.UNCOMMON));
            }

            MagicCardData.Rarity lastRarity = ThreadLocalRandom.current().nextInt(6) == 0
                    ? MagicCardData.Rarity.MYTHIC_RARE
                    : MagicCardData.Rarity.RARE;
            Optional<MagicCardData> lastCard = MagicCardPool.randomCard(lastRarity);
            if (lastCard.isEmpty() && lastRarity == MagicCardData.Rarity.MYTHIC_RARE) {
                // No mythic available locally (rare, but possible) - fall back to a rare card instead.
                lastCard = MagicCardPool.randomCard(MagicCardData.Rarity.RARE);
            }
            cards.add(lastCard.map(BoosterPackMenu::toCardStack).orElseGet(() -> new ItemStack(MinecraftTheGathering.MAGIC_CARD_ITEM.get())));

            packStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(cards));
        }

        packStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(container.getItems());
        return container;
    }

    // A random card of the given rarity from the local pool, or a blank (no-data) Magic Card if the pool
    // isn't loaded yet or has none of that rarity.
    private static ItemStack randomCardStack(MagicCardData.Rarity rarity) {
        return MagicCardPool.randomCard(rarity)
                .map(BoosterPackMenu::toCardStack)
                .orElseGet(() -> new ItemStack(MinecraftTheGathering.MAGIC_CARD_ITEM.get()));
    }

    private static ItemStack toCardStack(MagicCardData data) {
        ItemStack card = new ItemStack(MinecraftTheGathering.MAGIC_CARD_ITEM.get());
        card.set(MinecraftTheGathering.MAGIC_CARD_DATA.get(), data);
        return card;
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
        return !this.packStack.isEmpty();
    }

    // Closing an emptied-out pack destroys it: particles, the tool-break sound, and a shrink of the stack.
    @Override
    public void removed(Player player) {
        super.removed(player);
        this.packContainer.stopOpen(player);

        if (!player.level().isClientSide && !this.packStack.isEmpty() && this.packContainer.isEmpty()) {
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
            }

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            this.packStack.shrink(1);
        }
    }

    private static class MagicCardSlot extends Slot {
        MagicCardSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof MagicCardItem;
        }
    }
}
