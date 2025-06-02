package com.fabpharos.minecraftthegathering.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DataGeneration {
    public static void generate(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new ModBlockModels(packOutput, event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new ModItemModels(packOutput, event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(packOutput, "en_us"));

        ModBlockTags blockTags = new ModBlockTags(packOutput, lookupProvider, event.getExistingFileHelper());
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new ModItemTags(packOutput, lookupProvider, blockTags, event.getExistingFileHelper()));
        generator.addProvider(event.includeServer(), new ModRecipes(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Set.of(), List.of(
                //Add new Loot table providers here
                            new LootTableProvider.SubProviderEntry(
                                    ModBlockLootTableSubProvider::new,
                                    LootContextParamSets.BLOCK
                            )
                    ), lookupProvider)
        );

    }
}
