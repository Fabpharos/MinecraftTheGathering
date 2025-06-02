package com.fabpharos.minecraftthegathering.datagen;

import com.fabpharos.minecraftthegathering.MinecrafttheGathering;
import com.fabpharos.minecraftthegathering.Registration;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModels extends ItemModelProvider {

    public ModItemModels(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MinecrafttheGathering.MODID, existingFileHelper);
    }
    @Override
    protected void registerModels() {
        basicItem(Registration.CARD_ITEM.get());

        //withExistingParent(Registration.RAW_MATERIA_BLOCk.getId().getPath(), modLoc("block/raw_materia_block"));
    }
}
