package com.fabpharos.minecraftthegathering.item;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;
import com.fabpharos.minecraftthegathering.scryfall.MagicCardPool;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

public class MagicCardItem extends Item {
    // Matches each {...} symbol in a Scryfall mana cost string, e.g. "{2}{W}{U}".
    private static final Pattern MANA_SYMBOL_PATTERN = Pattern.compile("\\{([^}]+)}");

    public MagicCardItem(Properties properties) {
        super(properties);
    }

    // Stamps `faces` onto `stack` and picks the right texture for whichever face is now showing: the
    // "overlay" model for a real face (front of any card, or the back of a double-faced one), or the
    // plain default model for a blank back (see models/item/magic_card_item.json's overrides).
    public static void applyFaces(ItemStack stack, MagicCardFaces faces) {
        stack.set(MinecraftTheGathering.MAGIC_CARD_DATA.get(), faces);
        boolean showOverlay = !faces.currentFace().isBlank();
        stack.set(DataComponents.CUSTOM_MODEL_DATA, showOverlay ? new CustomModelData(1) : CustomModelData.DEFAULT);
    }

    @Override
    public Component getName(ItemStack stack) {
        MagicCardFaces faces = stack.get(MinecraftTheGathering.MAGIC_CARD_DATA.get());
        if (faces == null) {
            return super.getName(stack);
        }

        return faces.currentFace().isBlank()
                ? Component.translatable("item.minecraftthegathering.magic_card_item.face_down")
                : Component.literal(faces.currentFace().cardName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        MagicCardFaces faces = stack.get(MinecraftTheGathering.MAGIC_CARD_DATA.get());
        if (faces == null) {
            tooltipComponents.add(Component.translatable("tooltip.minecraftthegathering.magic_card_item.no_data"));
            return;
        }

        MagicCardData face = faces.currentFace();
        if (face.isBlank()) {
            // Face-down: nothing more to show, same as the back of a real card.
            return;
        }

        if (!face.manaCost().isBlank()) {
            tooltipComponents.add(renderManaCost(face.manaCost()));
        }

        if (!face.typeLine().isBlank()) {
            tooltipComponents.add(Component.literal(face.typeLine()));
        }

        // oracle_text commonly contains embedded newlines (multiple abilities); each needs its own tooltip line.
        for (String line : face.cardText().split("\n")) {
            tooltipComponents.add(Component.literal(line));
        }

        // A card is at most one of these: a creature (power/toughness), a planeswalker (loyalty),
        // or a battle (defense) - Scryfall only ever supplies the field(s) matching what the card actually is.
        if (face.power().isPresent() && face.toughness().isPresent()) {
            tooltipComponents.add(Component.literal(face.power().get() + "/" + face.toughness().get()));
        } else if (face.loyalty().isPresent()) {
            tooltipComponents.add(Component.translatable("tooltip.minecraftthegathering.magic_card_item.loyalty", face.loyalty().get()));
        } else if (face.defense().isPresent()) {
            tooltipComponents.add(Component.translatable("tooltip.minecraftthegathering.magic_card_item.defense", face.defense().get()));
        }

        MutableComponent rarityText = Component.translatable("rarity.minecraftthegathering." + face.rarity().getSerializedName());
        if (face.rarity().color() != null) {
            rarityText.withStyle(face.rarity().color());
        }
        tooltipComponents.add(rarityText);
    }

    // Renders a Scryfall mana cost string (e.g. "{2}{W}{U}") as "2 W U", coloring plain WUBRG symbols by color.
    private static Component renderManaCost(String manaCost) {
        MutableComponent line = Component.empty();
        Matcher matcher = MANA_SYMBOL_PATTERN.matcher(manaCost);
        boolean first = true;
        while (matcher.find()) {
            if (!first) {
                line.append(" ");
            }
            first = false;

            String symbol = matcher.group(1);
            MutableComponent symbolText = Component.literal(symbol);
            MagicCardData.Color color = MagicCardData.Color.fromManaLetter(symbol);
            if (color != null) {
                symbolText.withStyle(Style.EMPTY.withColor(color.rgb()));
            }

            line.append(symbolText);
        }

        return line;
    }

    // A card with no data yet picks a random one from the local card pool on right click. A card that
    // already has data instead flips between its front and back face. If there's more than one blank card
    // in the stack, only one is revealed: it's split off into its own stack rather than stamping the same
    // random card onto the whole stack.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            MagicCardFaces faces = stack.get(MinecraftTheGathering.MAGIC_CARD_DATA.get());
            if (faces == null) {
                MagicCardPool.randomCard().ifPresentOrElse(
                        revealed -> reveal(player, stack, revealed),
                        () -> MinecraftTheGathering.LOGGER.warn("No local Magic Card data available yet; the card pool may still be loading."));
            } else {
                applyFaces(stack, faces.toggled());
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void reveal(Player player, ItemStack stack, MagicCardFaces faces) {
        if (stack.getCount() > 1) {
            stack.shrink(1);
            ItemStack revealed = new ItemStack(this, 1);
            applyFaces(revealed, faces);
            if (!player.getInventory().add(revealed)) {
                player.drop(revealed, true);
            }
        } else {
            applyFaces(stack, faces);
        }
    }
}
