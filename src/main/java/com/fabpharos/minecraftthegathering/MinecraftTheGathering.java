package com.fabpharos.minecraftthegathering;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.fabpharos.minecraftthegathering.block.CardShredderBlock;
import com.fabpharos.minecraftthegathering.block.CardShredderBlockEntity;
import com.fabpharos.minecraftthegathering.command.ModCommands;
import com.fabpharos.minecraftthegathering.inventory.BoosterPackMenu;
import com.fabpharos.minecraftthegathering.inventory.CardShredderMenu;
import com.fabpharos.minecraftthegathering.item.BoosterPackItem;
import com.fabpharos.minecraftthegathering.item.BoosterPackSet;
import com.fabpharos.minecraftthegathering.item.MagicCardFaces;
import com.fabpharos.minecraftthegathering.item.MagicCardItem;
import com.fabpharos.minecraftthegathering.item.OpenedBoosterPackItem;
import com.fabpharos.minecraftthegathering.scryfall.ScryfallBulkDataService;
import com.fabpharos.minecraftthegathering.scryfall.ScryfallSetService;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MinecraftTheGathering.MODID)
public class MinecraftTheGathering {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "minecraftthegathering";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "minecraftthegathering" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "minecraftthegathering" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "minecraftthegathering" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold DataComponentTypes which will all be registered under the "minecraftthegathering" namespace
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    // Create a Deferred Register to hold MenuTypes which will all be registered under the "minecraftthegathering" namespace
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);
    // Create a Deferred Register to hold BlockEntityTypes which will all be registered under the "minecraftthegathering" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    // The data a Magic Card stores about itself: its front face, its back face, and which is showing (see MagicCardFaces)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MagicCardFaces>> MAGIC_CARD_DATA = DATA_COMPONENT_TYPES.registerComponentType(
            "magic_card_data", builder -> builder.persistent(MagicCardFaces.CODEC));

    // The (optional) Magic set a Booster Pack is themed to (see BoosterPackSet). Absent means "any set".
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BoosterPackSet>> BOOSTER_PACK_SET = DATA_COMPONENT_TYPES.registerComponentType(
            "booster_pack_set", builder -> builder.persistent(BoosterPackSet.CODEC));

    // The menu backing a Booster Pack's GUI (see BoosterPackMenu)
    public static final DeferredHolder<MenuType<?>, MenuType<BoosterPackMenu>> BOOSTER_PACK_MENU = MENU_TYPES.register(
            "booster_pack_menu", () -> new MenuType<>(BoosterPackMenu::new, FeatureFlags.VANILLA_SET));

    // The menu backing a Card Shredder's GUI (see CardShredderMenu)
    public static final DeferredHolder<MenuType<?>, MenuType<CardShredderMenu>> CARD_SHREDDER_MENU = MENU_TYPES.register(
            "card_shredder_menu", () -> new MenuType<>(CardShredderMenu::new, FeatureFlags.VANILLA_SET));

    // A machine that shreds Magic Cards into rarity-matched Scrap, then upgrades Scrap into Wildcards (see CardShredderBlockEntity).
    public static final DeferredBlock<CardShredderBlock> CARD_SHREDDER_BLOCK = BLOCKS.register(
            "card_shredder", () -> new CardShredderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F)));
    public static final DeferredItem<BlockItem> CARD_SHREDDER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("card_shredder", CARD_SHREDDER_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CardShredderBlockEntity>> CARD_SHREDDER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "card_shredder", () -> BlockEntityType.Builder.of(CardShredderBlockEntity::new, CARD_SHREDDER_BLOCK.get()).build(null));

    // A Magic: The Gathering card. Stores its name/text/rarity via MAGIC_CARD_DATA (see MagicCardItem).
    public static final DeferredItem<MagicCardItem> MAGIC_CARD_ITEM = ITEMS.registerItem(
            "magic_card_item", MagicCardItem::new, new Item.Properties().stacksTo(64));

    // A sealed pack of Magic Cards. Right-clicking breaks the seal and opens its GUI (see BoosterPackItem).
    public static final DeferredItem<BoosterPackItem> BOOSTER_PACK_ITEM = ITEMS.registerItem(
            "booster_pack_item", BoosterPackItem::new, new Item.Properties().stacksTo(1));

    // What a Booster Pack becomes once its seal is broken. Right-clicking reopens its GUI (see OpenedBoosterPackItem).
    public static final DeferredItem<OpenedBoosterPackItem> OPENED_BOOSTER_PACK_ITEM = ITEMS.registerItem(
            "opened_booster_pack_item", OpenedBoosterPackItem::new, new Item.Properties().stacksTo(1));

    // Raw material a Card Shredder produces from a Magic Card of the matching rarity; 8 of one combine into its Wildcard.
    public static final DeferredItem<Item> COMMON_SCRAP_ITEM = ITEMS.registerSimpleItem("common_scrap");
    public static final DeferredItem<Item> UNCOMMON_SCRAP_ITEM = ITEMS.registerSimpleItem("uncommon_scrap");
    public static final DeferredItem<Item> RARE_SCRAP_ITEM = ITEMS.registerSimpleItem("rare_scrap");
    public static final DeferredItem<Item> MYTHIC_RARE_SCRAP_ITEM = ITEMS.registerSimpleItem("mythic_rare_scrap");

    // Made from 8 Scrap of the matching rarity, either by hand (see data/.../recipe) or by a Card Shredder.
    public static final DeferredItem<Item> COMMON_WILDCARD_ITEM = ITEMS.registerSimpleItem("common_wildcard");
    public static final DeferredItem<Item> UNCOMMON_WILDCARD_ITEM = ITEMS.registerSimpleItem("uncommon_wildcard");
    public static final DeferredItem<Item> RARE_WILDCARD_ITEM = ITEMS.registerSimpleItem("rare_wildcard");
    public static final DeferredItem<Item> MYTHIC_RARE_WILDCARD_ITEM = ITEMS.registerSimpleItem("mythic_rare_wildcard");

    // Creates a creative tab with the id "minecraftthegathering:items" for this mod's items, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEMS_TAB = CREATIVE_MODE_TABS.register("items", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.minecraftthegathering")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> MAGIC_CARD_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(MAGIC_CARD_ITEM.get());
                output.accept(BOOSTER_PACK_ITEM.get());
                output.accept(CARD_SHREDDER_BLOCK_ITEM.get());
                output.accept(COMMON_SCRAP_ITEM.get());
                output.accept(UNCOMMON_SCRAP_ITEM.get());
                output.accept(RARE_SCRAP_ITEM.get());
                output.accept(MYTHIC_RARE_SCRAP_ITEM.get());
                output.accept(COMMON_WILDCARD_ITEM.get());
                output.accept(UNCOMMON_WILDCARD_ITEM.get());
                output.accept(RARE_WILDCARD_ITEM.get());
                output.accept(MYTHIC_RARE_WILDCARD_ITEM.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public MinecraftTheGathering(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so data component types get registered
        DATA_COMPONENT_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so menu types get registered
        MENU_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entity types get registered
        BLOCK_ENTITY_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (MinecraftTheGathering) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");

        ScryfallBulkDataService.ensureCardDataReady(event.getServer());
        ScryfallSetService.ensureSetDataReady(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event);
    }
}
