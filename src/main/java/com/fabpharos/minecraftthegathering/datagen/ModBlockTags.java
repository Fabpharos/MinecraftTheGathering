package com.fabpharos.minecraftthegathering.datagen;

import com.fabpharos.minecraftthegathering.MinecrafttheGathering;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTags extends BlockTagsProvider {
    public ModBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, MinecrafttheGathering.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        //tag(BlockTags.MINEABLE_WITH_PICKAXE).add(Registration.RAW_MATERIA_BLOCk.get());
        //tag(BlockTags.NEEDS_IRON_TOOL).add(Registration.RAW_MATERIA_BLOCk.get());
    }
}
