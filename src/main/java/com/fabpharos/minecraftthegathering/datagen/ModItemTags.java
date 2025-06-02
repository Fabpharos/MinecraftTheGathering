package com.fabpharos.minecraftthegathering.datagen;

import com.fabpharos.minecraftthegathering.MinecrafttheGathering;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModItemTags extends ItemTagsProvider {
    public ModItemTags(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider, ModBlockTags blockTags, ExistingFileHelper existingFileHelper) {
        super(packOutput, lookupProvider, blockTags.contentsGetter(), MinecrafttheGathering.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

    }
}
