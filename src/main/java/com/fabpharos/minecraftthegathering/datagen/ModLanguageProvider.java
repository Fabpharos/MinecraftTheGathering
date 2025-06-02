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

        //Tabs
        add("tab.minecraftthegathering", "Minecraft: The Gathering");
        //Menu Elements
        /*
        add("key.materia.melding", "Melding Menu");
        add("tooltip.materia.column.top", "┌  %s");
        add("tooltip.materia.column.middle", "├  %s");
        add("tooltip.materia.column.bottom", "└  %s");
        add("tooltip.materia.empty", "Empty");
        add("info.materia.level", "Level %s");
        add("info.materia.ap", "AP: %s/%s");
        */
    }
}
