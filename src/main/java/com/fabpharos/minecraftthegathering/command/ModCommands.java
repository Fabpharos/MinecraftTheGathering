package com.fabpharos.minecraftthegathering.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import com.fabpharos.minecraftthegathering.MinecraftTheGathering;
import com.fabpharos.minecraftthegathering.item.BoosterPackItem;
import com.fabpharos.minecraftthegathering.item.BoosterPackSet;
import com.fabpharos.minecraftthegathering.item.MagicCardFaces;
import com.fabpharos.minecraftthegathering.item.MagicCardItem;
import com.fabpharos.minecraftthegathering.item.OpenedBoosterPackItem;
import com.fabpharos.minecraftthegathering.scryfall.MagicCardPool;
import com.fabpharos.minecraftthegathering.scryfall.MagicSetPool;
import com.fabpharos.minecraftthegathering.scryfall.ScryfallBulkDataService;
import com.fabpharos.minecraftthegathering.scryfall.ScryfallSetService;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** This mod's admin/testing commands, rooted at {@code /minecraftthegathering}. */
public final class ModCommands {
    // The MTG card types this mod recognizes for /minecraftthegathering giverandom. Matched as a
    // case-insensitive substring of a card's type line, so "creature" also matches "Legendary Creature".
    private static final String[] CARD_TYPES = {
            "artifact", "battle", "creature", "enchantment", "instant", "kindred", "land", "planeswalker", "sorcery"
    };

    private ModCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(MinecraftTheGathering.MODID)
                .then(Commands.literal("downloadcarddata")
                        .requires(source -> source.hasPermission(2))
                        .executes(ModCommands::executeDownloadCardData))
                .then(Commands.literal("downloadsetdata")
                        .requires(source -> source.hasPermission(2))
                        .executes(ModCommands::executeDownloadSetData))
                .then(Commands.literal("give")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ModCommands::executeGiveByName))))
                .then(Commands.literal("giverandom")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(CARD_TYPES, builder))
                                        .executes(ModCommands::executeGiveRandomByType))))
                .then(Commands.literal("assignset")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("set", StringArgumentType.greedyString())
                                        .suggests(ModCommands::suggestSets)
                                        .executes(ModCommands::executeAssignSet)))));
    }

    // Manually (re)downloads the Scryfall bulk card data, regardless of whether it already exists.
    private static int executeDownloadCardData(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (ScryfallBulkDataService.isDownloadInProgress()) {
            source.sendFailure(Component.literal("A Scryfall card data download is already in progress."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Downloading Magic: The Gathering card data from Scryfall..."), true);
        ScryfallBulkDataService.downloadNow(source.getServer(), success -> {
            if (success) {
                source.sendSuccess(() -> Component.literal("Scryfall card data downloaded successfully."), true);
            } else {
                source.sendFailure(Component.literal("Failed to download Scryfall card data. Check the server log for details."));
            }
        });

        return Command.SINGLE_SUCCESS;
    }

    // Manually (re)downloads the Scryfall set data, regardless of whether it already exists.
    private static int executeDownloadSetData(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (ScryfallSetService.isDownloadInProgress()) {
            source.sendFailure(Component.literal("A Scryfall set data download is already in progress."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Downloading Magic: The Gathering set data from Scryfall..."), true);
        ScryfallSetService.downloadNow(source.getServer(), success -> {
            if (success) {
                source.sendSuccess(() -> Component.literal("Scryfall set data downloaded successfully."), true);
            } else {
                source.sendFailure(Component.literal("Failed to download Scryfall set data. Check the server log for details."));
            }
        });

        return Command.SINGLE_SUCCESS;
    }

    // Fuzzy-searches the local card pool by name and gives the single best match.
    private static int executeGiveByName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String query = StringArgumentType.getString(context, "name");
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        CommandSourceStack source = context.getSource();

        Optional<MagicCardFaces> match = MagicCardPool.findByName(query);
        if (match.isEmpty()) {
            source.sendFailure(Component.literal("No card resembling \"" + query + "\" was found in the local card pool."));
            return 0;
        }

        return giveToAll(source, targets, match.get());
    }

    // Gives a random card whose type line matches the selected card type.
    private static int executeGiveRandomByType(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String type = StringArgumentType.getString(context, "type");
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        CommandSourceStack source = context.getSource();

        Optional<MagicCardFaces> match = MagicCardPool.randomCardOfType(type);
        if (match.isEmpty()) {
            source.sendFailure(Component.literal("No " + type + " cards were found in the local card pool."));
            return 0;
        }

        return giveToAll(source, targets, match.get());
    }

    private static int giveToAll(CommandSourceStack source, Collection<ServerPlayer> targets, MagicCardFaces faces) {
        for (ServerPlayer player : targets) {
            ItemStack card = new ItemStack(MinecraftTheGathering.MAGIC_CARD_ITEM.get());
            MagicCardItem.applyFaces(card, faces);
            if (!player.getInventory().add(card)) {
                player.drop(card, false);
            }
        }

        source.sendSuccess(() -> Component.literal("Gave " + targets.size() + " player(s) \"" + faces.front().cardName() + "\"."), true);
        return targets.size();
    }

    // Suggests every known set's code and name (e.g. "fdn", "Foundations") for tab-completion.
    private static CompletableFuture<Suggestions> suggestSets(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> options = new ArrayList<>();
        for (MagicSetPool.MagicSet set : MagicSetPool.all()) {
            options.add(set.code());
            options.add(set.name());
        }

        return SharedSuggestionProvider.suggest(options, builder);
    }

    // Assigns a set (by code or, failing that, best fuzzy name match) to the Booster Pack each target
    // player is holding in their main hand. Targets not holding one are silently skipped.
    private static int executeAssignSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String query = StringArgumentType.getString(context, "set");
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        CommandSourceStack source = context.getSource();

        Optional<MagicSetPool.MagicSet> match = MagicSetPool.find(query);
        if (match.isEmpty()) {
            source.sendFailure(Component.literal("No set matching \"" + query + "\" was found in the local set pool."));
            return 0;
        }

        MagicSetPool.MagicSet set = match.get();
        BoosterPackSet packSet = new BoosterPackSet(Optional.of(set.code()), Optional.of(set.name()), Optional.of(set.id()));

        int assigned = 0;
        for (ServerPlayer player : targets) {
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof BoosterPackItem || held.getItem() instanceof OpenedBoosterPackItem) {
                held.set(MinecraftTheGathering.BOOSTER_PACK_SET.get(), packSet);
                assigned++;
            }
        }

        if (assigned == 0) {
            source.sendFailure(Component.literal("No targeted player is holding a Booster Pack in their main hand."));
            return 0;
        }

        int finalAssigned = assigned;
        source.sendSuccess(() -> Component.literal("Assigned set \"" + set.name() + "\" to " + finalAssigned + " Booster Pack(s)."), true);
        return assigned;
    }
}
