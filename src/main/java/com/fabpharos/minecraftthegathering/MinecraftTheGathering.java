package com.fabpharos.minecraftthegathering;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.fabpharos.minecraftthegathering.inventory.BoosterPackMenu;
import com.fabpharos.minecraftthegathering.item.BoosterPackItem;
import com.fabpharos.minecraftthegathering.item.MagicCardData;
import com.fabpharos.minecraftthegathering.item.MagicCardItem;
import com.fabpharos.minecraftthegathering.item.OpenedBoosterPackItem;
import com.fabpharos.minecraftthegathering.scryfall.ScryfallBulkDataService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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

    // The data a Magic Card stores about itself: name, rules text, and rarity
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MagicCardData>> MAGIC_CARD_DATA = DATA_COMPONENT_TYPES.registerComponentType(
            "magic_card_data", builder -> builder.persistent(MagicCardData.CODEC));

    // The menu backing a Booster Pack's GUI (see BoosterPackMenu)
    public static final DeferredHolder<MenuType<?>, MenuType<BoosterPackMenu>> BOOSTER_PACK_MENU = MENU_TYPES.register(
            "booster_pack_menu", () -> new MenuType<>(BoosterPackMenu::new, FeatureFlags.VANILLA_SET));

    // Creates a new Block with the id "minecraftthegathering:test_block", combining the namespace and path
    public static final DeferredBlock<Block> TEST_BLOCK = BLOCKS.registerSimpleBlock("test_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "minecraftthegathering:test_block", combining the namespace and path
    public static final DeferredItem<BlockItem> TEST_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("test_block", TEST_BLOCK);

    // Creates a new food item with the id "minecraftthegathering:test_item", nutrition 1 and saturation 2
    public static final DeferredItem<Item> TEST_ITEM = ITEMS.registerSimpleItem("test_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // A Magic: The Gathering card. Stores its name/text/rarity via MAGIC_CARD_DATA (see MagicCardItem).
    public static final DeferredItem<MagicCardItem> MAGIC_CARD_ITEM = ITEMS.registerItem(
            "magic_card_item", MagicCardItem::new, new Item.Properties().stacksTo(64));

    // A sealed pack of Magic Cards. Right-clicking breaks the seal and opens its GUI (see BoosterPackItem).
    public static final DeferredItem<BoosterPackItem> BOOSTER_PACK_ITEM = ITEMS.registerItem(
            "booster_pack_item", BoosterPackItem::new, new Item.Properties().stacksTo(1));

    // What a Booster Pack becomes once its seal is broken. Right-clicking reopens its GUI (see OpenedBoosterPackItem).
    public static final DeferredItem<OpenedBoosterPackItem> OPENED_BOOSTER_PACK_ITEM = ITEMS.registerItem(
            "opened_booster_pack_item", OpenedBoosterPackItem::new, new Item.Properties().stacksTo(1));

    // Creates a creative tab with the id "minecraftthegathering:test_tab" for the test item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TEST_TAB = CREATIVE_MODE_TABS.register("test_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.minecraftthegathering")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> TEST_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(TEST_ITEM.get());// Add the test item to the tab. For your own tabs, this method is preferred over the event
                output.accept(TEST_BLOCK_ITEM.get());// Add the test block to the tab
                output.accept(MAGIC_CARD_ITEM.get());
                output.accept(BOOSTER_PACK_ITEM.get());
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

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (MinecraftTheGathering) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

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

    // Add the test block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(TEST_BLOCK_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");

        ScryfallBulkDataService.ensureCardDataReady(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(MODID)
                .then(Commands.literal("downloadcarddata")
                        .requires(source -> source.hasPermission(2))
                        .executes(this::executeDownloadCardData)));
    }

    // Manually (re)downloads the Scryfall bulk card data, regardless of whether it already exists.
    private int executeDownloadCardData(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (ScryfallBulkDataService.isDownloadInProgress()) {
            source.sendFailure(Component.literal("A Scryfall card data download is already in progress."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Downloading Magic: The Gathering card data from Scryfall..."), true);
        ScryfallBulkDataService.downloadNow(source.getServer(), success -> {
            if (success) {
                source.sendSuccess(() -> Component.literal("Scryfall card data downloaded successfully."), true);
            } else {
                source.sendFailure(Component.literal("Failed to download Scryfall card data. Check the server log for details."));
            }
        });

        return Command.SINGLE_SUCCESS;
    }
}
