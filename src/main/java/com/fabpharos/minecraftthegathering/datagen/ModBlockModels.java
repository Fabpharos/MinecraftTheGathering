package com.fabpharos.minecraftthegathering.datagen;

import com.fabpharos.minecraftthegathering.MinecrafttheGathering;
import com.fabpharos.minecraftthegathering.Registration;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockModels extends BlockStateProvider {

    public ModBlockModels(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MinecrafttheGathering.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {

        //simpleBlock(Registration.RAW_MATERIA_BLOCk.get());
    }
}
