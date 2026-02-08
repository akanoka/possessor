package com.example.possessor;

import com.example.possessor.game.GameManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;

public class PossessorCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("possessor")
            .requires(source -> source.hasPermission(0))
            .then(Commands.literal("start")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    GameManager.getInstance().startGame(context.getSource().getServer());
                    return 1;
                }))
            .then(Commands.literal("stop")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    GameManager.getInstance().stopGame(context.getSource().getServer());
                    return 1;
                }))
            .then(Commands.literal("skip")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    GameManager.getInstance().skipPhase();
                    context.getSource().sendSuccess(() -> Component.literal("Phase passée !").withStyle(ChatFormatting.GREEN), true);
                    return 1;
                }))
            .then(Commands.literal("select")
                .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer source = context.getSource().getPlayerOrException();
                        ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");

                        if (GameManager.getInstance().getCurrentPhase() != com.example.possessor.game.GamePhase.SELECTION) {
                            context.getSource().sendFailure(Component.literal("La sélection n'est pas active !"));
                            return 0;
                        }

                        if (com.example.possessor.game.RoleManager.getInstance().getRole(source) != com.example.possessor.game.Role.POSSESSOR) {
                            context.getSource().sendFailure(Component.literal("Seul le Possesseur peut choisir !"));
                            return 0;
                        }
                        
                        GameManager.getInstance().setPendingTarget(target.getUUID());
                        context.getSource().sendSuccess(() -> Component.literal("Cible sélectionnée : " + target.getName().getString()), false);
                        return 1;
                    })))
            .then(Commands.literal("vote")
                .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer voter = context.getSource().getPlayerOrException();
                        ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
                        
                        if (GameManager.getInstance().getCurrentPhase() != com.example.possessor.game.GamePhase.VOTING) {
                            context.getSource().sendFailure(Component.literal("Le vote n'est pas actif !"));
                            return 0;
                        }
                        
                        com.example.possessor.game.VoteManager.getInstance().castVote(voter.getUUID(), target.getUUID());
                        context.getSource().sendSuccess(() -> Component.literal("A voté pour " + target.getName().getString()), false);
                        
                        GameManager.getInstance().syncVotes(context.getSource().getServer());
                        return 1;
                    })))
            .then(Commands.literal("role")
                .executes(context -> {
                     ServerPlayer player = context.getSource().getPlayerOrException();
                     com.example.possessor.game.Role role = com.example.possessor.game.RoleManager.getInstance().getRole(player);
                     
                     ChatFormatting color = ChatFormatting.GRAY;
                     if (role == com.example.possessor.game.Role.INNOCENT) color = ChatFormatting.GREEN;
                     else if (role == com.example.possessor.game.Role.POSSESSOR) color = ChatFormatting.RED;
                     
                     player.sendSystemMessage(Component.literal("Votre Rôle : " + role.name()).withStyle(color, ChatFormatting.BOLD));
                     return 1;
                }))
            .then(Commands.literal("reset")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    GameManager.getInstance().stopGame(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal("Système Possesseur réinitialisé."), true);
                    return 1;
                }))
            .then(Commands.literal("setrole")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                    .then(Commands.argument("role", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (com.example.possessor.game.Role role : com.example.possessor.game.Role.values()) {
                                builder.suggest(role.name());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "player");
                            String roleName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "role");
                            try {
                                com.example.possessor.game.Role role = com.example.possessor.game.Role.valueOf(roleName.toUpperCase());
                                com.example.possessor.game.RoleManager.getInstance().setRole(target, role);
                                context.getSource().sendSuccess(() -> Component.literal("Rôle de " + target.getName().getString() + " mis à " + role.name()), true);
                                GameManager.getInstance().syncState(context.getSource().getServer());
                                return 1;
                            } catch (IllegalArgumentException e) {
                                context.getSource().sendFailure(Component.literal("Rôle invalide !"));
                                return 0;
                            }
                        })))));
    }
}
