package com.example.possessor.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class DebugLogger {
    
    public static void log(String message) {
        if (ConfigManager.isDebug()) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Component debugMsg = Component.literal("[DEBUG] ").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
                    .append(Component.literal(message).withStyle(ChatFormatting.AQUA));
                
                server.getPlayerList().broadcastSystemMessage(debugMsg, false);
            }
            System.out.println("[PossessorMod DEBUG] " + message);
        }
    }

    public static void error(String message) {
        if (ConfigManager.isDebug()) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Component errorMsg = Component.literal("[DEBUG ERROR] ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                    .append(Component.literal(message).withStyle(ChatFormatting.RED));
                
                server.getPlayerList().broadcastSystemMessage(errorMsg, false);
            }
            System.err.println("[PossessorMod ERROR] " + message);
        }
    }
}
