package com.fabpharos.minecraftthegathering;

import com.fabpharos.minecraftthegathering.item.BoosterPackItem;
import com.fabpharos.minecraftthegathering.item.CardItem;
import com.fabpharos.minecraftthegathering.item.CardItemComponent;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.fabpharos.minecraftthegathering.MinecrafttheGathering.MODID;

public class Registration {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    //Blocks

    //Items
    public static final DeferredItem<CardItem> CARD_ITEM = ITEMS.register("magic_card_item", (ResourceLocation properties) -> new CardItem());
    public static final DeferredItem<BoosterPackItem> BOOSTER_PACK_ITEM = ITEMS.register("booster_pack_item", (ResourceLocation properties) -> new BoosterPackItem());

    //Creative tab
    public static Supplier<CreativeModeTab> TAB = TABS.register(MODID, () -> CreativeModeTab.builder()
            .title(Component.translatable("tab.minecraftthegathering"))
            .icon(() -> new ItemStack(CARD_ITEM.get()))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .displayItems((featureFlags, output) -> {
                output.accept(CARD_ITEM.get());
            })
            .build());

    //Components
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CardItemComponent>> CARD_ITEM_COMPONENTS = DATA_COMPONENTS.registerComponentType(
            "card_item_components",
            builder -> builder
                    .persistent(CardItemComponent.CODEC)
                    .networkSynchronized(CardItemComponent.STREAM_CODEC)
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> BOOSTER_PACK_CONTENTS = DATA_COMPONENTS.registerComponentType(
            "booster_pack_contents",
            builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
    );

    public static void init(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
    }
}
