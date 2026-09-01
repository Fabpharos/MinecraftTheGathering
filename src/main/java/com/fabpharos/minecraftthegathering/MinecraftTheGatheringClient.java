package com.fabpharos.minecraftthegathering;

import com.fabpharos.minecraftthegathering.client.BoosterPackScreen;
import com.fabpharos.minecraftthegathering.client.CardShredderScreen;
import com.fabpharos.minecraftthegathering.item.MagicCardFaces;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = MinecraftTheGathering.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = MinecraftTheGathering.MODID, value = Dist.CLIENT)
public class MinecraftTheGatheringClient {
    public MinecraftTheGatheringClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        MinecraftTheGathering.LOGGER.info("HELLO FROM CLIENT SETUP");
        MinecraftTheGathering.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MinecraftTheGathering.BOOSTER_PACK_MENU.get(), BoosterPackScreen::new);
        event.register(MinecraftTheGathering.CARD_SHREDDER_MENU.get(), CardShredderScreen::new);
    }

    // Tints the magic_card_item_overlay model's layer0 (the plain card-art base, magic_card.png) by the
    // current face's color identity. Only that model uses a real card-art layer0 - the default
    // (not-yet-revealed / face-down) model's layer0 is the generic magic_card_item icon, which must stay
    // untinted, so this returns -1 (no tint) whenever that's what's actually showing.
    @SubscribeEvent
    static void onRegisterColorHandlers(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }

            MagicCardFaces faces = stack.get(MinecraftTheGathering.MAGIC_CARD_DATA.get());
            if (faces == null || faces.currentFace().isBlank()) {
                return -1;
            }

            return 0xFF000000 | faces.currentFace().frameColorRgb();
        }, MinecraftTheGathering.MAGIC_CARD_ITEM.get());
    }
}
