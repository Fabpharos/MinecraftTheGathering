package com.fabpharos.minecraftthegathering.util;

import com.fabpharos.minecraftthegathering.Registration;
import com.fabpharos.minecraftthegathering.item.CardItemComponent;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;

public class MagicCardUtils {
    public static void assignCard(ItemStack stack, JsonObject json, boolean foil) {
        stack.applyComponents(DataComponentMap.builder()
                .set(Registration.CARD_ITEM_COMPONENTS.get(), new CardItemComponent(
                        json.has("name") ? json.get("name").getAsString() : "",
                        json.has("mana_cost") ? json.get("mana_cost").getAsString() : "",
                        json.has("type_line") ? json.get("type_line").getAsString() : "",
                        json.has("oracle_text") ? json.get("oracle_text").getAsString() : "",
                        json.has("power") ? json.get("power").getAsString() : "",
                        json.has("toughness") ? json.get("toughness").getAsString() : "",
                        json.has("loyalty") ? json.get("loyalty").getAsString() : "",
                        json.getAsJsonObject("image_uris").get("small").getAsString(),
                        json.has("colors") ? json.get("colors").getAsJsonArray().get(0).getAsString() : "",
                        foil
                )).build()
        );
    }
}
