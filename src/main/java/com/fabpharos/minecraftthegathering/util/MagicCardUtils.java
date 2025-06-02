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
                        /*
    private final String cardName;
    private final String manaCost;
    private final String typeLine;
    private final String cardText;

    private final String power;
    private final String toughness;
    private final String loyalty;
    private final String image;

    private final String color;
    private final boolean foil;
                        */
                        json.get("name").getAsString(),
                        json.get("mana_cost").getAsString(),
                        json.get("type_line").getAsString(),
                        json.get("oracle_text").getAsString(),
                        json.has("power") ? json.get("power").getAsString() : "",
                        json.has("toughness") ? json.get("toughness").getAsString() : "",
                        json.has("loyalty") ? json.get("loyalty").getAsString() : "",
                        json.getAsJsonObject("image_uris").get("small").getAsString(),
                        json.get("colors").getAsString(),
                        foil
                )).build()
        );
    }
}
