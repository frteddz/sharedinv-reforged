package com.t3ddz.sharedinv_reforged;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class SharedInventoryCommand {
    public static SuggestionProvider<CommandSourceStack> inventoriesProvider = (context, builder) ->
            SharedSuggestionProvider.suggest(SharedInventoryManager.inventories.keySet().toArray(String[]::new), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal("sharedinv")
                        .requires(source -> source.hasPermission(Commands.LEVEL_MODERATORS))
                        .then(literal("help")
                                .executes(context -> {
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("Find help at: ").append(
                                                    Component.literal("https://modrinth.com/mod/sharedinv")
                                                            .setStyle(Style.EMPTY
                                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/sharedinv"))
                                                                    .withUnderlined(true)
                                                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open the Shared Inventory mod page on Modrinth")))
                                                            )
                                            ), false);
                                    return 1;
                                })
                        )
                        .then(literal("group")
                                .then(literal("add")
                                        .then(argument("name", StringArgumentType.word())
                                                .executes(SharedInventoryCommand::executeAdd)
                                        )
                                )
                                .then(literal("remove")
                                        .then(argument("inventory", StringArgumentType.word()).suggests(inventoriesProvider)
                                                .executes(SharedInventoryCommand::executeRemove)
                                        )
                                )
                                .then(literal("list")
                                        .executes(SharedInventoryCommand::executeListInventories)
                                        .then(argument("inventory", StringArgumentType.string()).suggests(inventoriesProvider)
                                                .executes(SharedInventoryCommand::executeListInventoryPlayers)
                                        )
                                )
                                .then(literal("join")
                                        .then(argument("inventory", StringArgumentType.string()).suggests(inventoriesProvider)
                                                .executes(SharedInventoryCommand::executeJoinSelf)
                                                .then(argument("player", GameProfileArgument.gameProfile())
                                                        .executes(SharedInventoryCommand::executeJoinPlayers)
                                                )
                                        )
                                )
                                .then(literal("leave")
                                        .then(argument("player", GameProfileArgument.gameProfile())
                                                .executes(SharedInventoryCommand::executeLeave)
                                        )
                                )
                                .then(literal("default")
                                        .then(literal("set")
                                                .then(argument("inventory", StringArgumentType.string()).suggests(inventoriesProvider)
                                                        .executes(SharedInventoryCommand::executeDefaultSet)
                                                )
                                        )
                                        .then(literal("get")
                                                .executes(SharedInventoryCommand::executeDefaultGet)
                                        )
                                        .then(literal("clear")
                                                .executes(SharedInventoryCommand::executeDefaultRemove)
                                        )
                                )
                        )
        );
    }

    public static int executeAdd(CommandContext<CommandSourceStack> context) {
        String invName = StringArgumentType.getString(context, "name");
        if (SharedInventoryManager.inventories.get(invName) != null) {
            context.getSource().sendFailure(Component.literal("An inventory with that name already exists!"));
            return 0;
        }

        SharedInventory inv = new SharedInventory(invName);
        SharedInventoryManager.inventories.put(inv.name, inv);

        context.getSource().sendSuccess(() -> Component.literal(String.format("Created inventory '%s'", inv.name)), true);
        return 1;
    }

    public static int executeRemove(CommandContext<CommandSourceStack> context) {
        String invName = StringArgumentType.getString(context, "inventory");
        SharedInventory inv = SharedInventoryManager.inventories.get(invName);
        if (inv == null) {
            context.getSource().sendFailure(Component.literal(String.format("Unknown inventory '%s'", invName)));
            return 0;
        }

        while (!inv.players.isEmpty()) {
            UUID uuid = inv.players.iterator().next();
            inv.removePlayer(uuid);
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(uuid);
            if (player == null) continue;
            SharedInventoryManager.restorePlayerSlots(player);
        }
        SharedInventoryManager.inventories.remove(invName);

        context.getSource().sendSuccess(() -> Component.literal(String.format("Removed inventory %s", invName)), true);
        return 1;
    }

    public static int executeJoinSelf(CommandContext<CommandSourceStack> context) {
        String invName = StringArgumentType.getString(context, "inventory");
        SharedInventory inv = SharedInventoryManager.inventories.get(invName);
        if (inv == null) {
            context.getSource().sendFailure(Component.literal(String.format("Unknown inventory '%s'", invName)));
            return 0;
        }

        ServerPlayer player = context.getSource().getPlayer();
        inv.addPlayer(player.getUUID());
        ((IPlayerInventory) player.getInventory()).sharedinv$updateFrom(inv);

        context.getSource().sendSuccess(() -> Component.literal(String.format(
                "%s is now sharing the inventory '%s'", player.getGameProfile().getName(), inv.name)), true);
        return 1;
    }

    public static int executeJoinPlayers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String invName = StringArgumentType.getString(context, "inventory");
        SharedInventory inv = SharedInventoryManager.inventories.get(invName);
        if (inv == null) {
            context.getSource().sendFailure(Component.literal(String.format("Unknown inventory '%s'", invName)));
            return 0;
        }

        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
        profiles.forEach(profile -> {
            inv.addPlayer(profile.getId());
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(profile.getId());
            if (player != null) {
                ((IPlayerInventory) player.getInventory()).sharedinv$updateFrom(inv);
            }
        });

        if (profiles.size() == 1) {
            context.getSource().sendSuccess(() -> Component.literal(String.format(
                    "%s is now sharing the inventory '%s'", profiles.stream().findFirst().orElseThrow().getName(), inv.name)), true);
        } else {
            context.getSource().sendSuccess(() -> Component.literal(String.format(
                    "Shared inventory '%s' with %d players", inv.name, profiles.size())), true);
        }
        return 1;
    }

    public static int executeLeave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
        profiles.forEach(profile -> {
            SharedInventory inv = SharedInventory.playerInvs.get(profile.getId());
            if (inv == null) return;
            inv.removePlayer(profile.getId());
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(profile.getId());
            if (player != null) {
                SharedInventoryManager.restorePlayerSlots(player);
            }
        });

        if (profiles.size() == 1) {
            context.getSource().sendSuccess(() -> Component.literal(String.format(
                    "%s is no longer sharing an inventory", profiles.stream().findFirst().orElseThrow().getName())), true);
        } else {
            context.getSource().sendSuccess(() -> Component.literal(String.format(
                    "%d players are no longer sharing an inventory", profiles.size())), true);
        }
        return 1;
    }

    public static int executeListInventories(CommandContext<CommandSourceStack> context) {
        if (SharedInventoryManager.inventories.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("There are no shared inventories"), false);
        } else {
            context.getSource().sendSuccess(() -> {
                Set<String> names = SharedInventoryManager.inventories.keySet();
                return Component.literal(String.format(
                        "There %s %d shared %s: %s",
                        names.size() > 1 ? "are" : "is",
                        names.size(),
                        names.size() > 1 ? "inventories" : "inventory",
                        String.join(", ", names)
                ));
            }, false);
        }
        return 1;
    }

    public static int executeListInventoryPlayers(CommandContext<CommandSourceStack> context) {
        String invName = StringArgumentType.getString(context, "inventory");
        SharedInventory inv = SharedInventoryManager.inventories.get(invName);
        if (inv == null) {
            context.getSource().sendFailure(Component.literal(String.format("Unknown inventory '%s'", invName)));
            return 0;
        }

        if (inv.players.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal(String.format(
                    "There are no players sharing the inventory '%s'", inv.name)), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal(String.format(
                    "There %s %d player%s sharing the inventory '%s': %s",
                    inv.players.size() > 1 ? "are" : "is",
                    inv.players.size(),
                    inv.players.size() > 1 ? "s" : "",
                    inv.name,
                    inv.players.stream()
                            .map(uuid -> context.getSource().getServer().getProfileCache().get(uuid)
                                    .map(GameProfile::getName)
                                    .orElseGet(uuid::toString))
                            .collect(Collectors.joining(", "))
            )), false);
        }
        return 1;
    }

    public static int executeDefaultSet(CommandContext<CommandSourceStack> context) {
        String invName = StringArgumentType.getString(context, "inventory");
        SharedInventory inv = SharedInventoryManager.inventories.get(invName);
        if (inv == null) {
            context.getSource().sendFailure(Component.literal(String.format("Unknown inventory '%s'", invName)));
            return 0;
        }

        SharedInventoryManager.defaultInv = inv;
        context.getSource().sendSuccess(() -> Component.literal("Set default shared inventory to: " + inv.name), true);
        return 1;
    }

    public static int executeDefaultGet(CommandContext<CommandSourceStack> context) {
        if (SharedInventoryManager.defaultInv != null) {
            context.getSource().sendSuccess(() -> Component.literal("The default inventory is: " + SharedInventoryManager.defaultInv.name), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("There is no default inventory."), false);
        }
        return 1;
    }

    public static int executeDefaultRemove(CommandContext<CommandSourceStack> context) {
        if (SharedInventoryManager.defaultInv != null) {
            SharedInventoryManager.defaultInv = null;
            context.getSource().sendSuccess(() -> Component.literal("Removed default shared inventory"), true);
        } else {
            context.getSource().sendFailure(Component.literal("There is no default inventory to remove!"));
        }
        return 1;
    }
}
