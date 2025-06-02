package com.fabpharos.minecraftthegathering.datagen;

import com.fabpharos.minecraftthegathering.MinecrafttheGathering;
import com.fabpharos.minecraftthegathering.Registration;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput packOutput, String locale) {
        super(packOutput, MinecrafttheGathering.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        //Blocks
        //add(Registration.RAW_MATERIA_BLOCk.get(), "Raw Materia Block");

        //Items
        add(Registration.CARD_ITEM.get(), "Card");
        add(Registration.BOOSTER_PACK_ITEM.get(), "Booster Pack");

        //Tabs
        add("tab.minecraftthegathering", "Minecraft: The Gathering");
        //Menu Elements
        add("tooltip.minecraftthegathering.card_item_message", "Hold shift to view card text");
    }
}
