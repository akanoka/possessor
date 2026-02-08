package com.example.possessor.compat;

import com.example.possessor.game.DebugLogger;
import net.minecraft.server.MinecraftServer;
import java.lang.reflect.Method;
import java.util.UUID;

public class SkinRestorerCompat {
    
    public static void setSkin(MinecraftServer server, UUID playerUuid, String skinName) {
        try {
            // Attempt 1: Suiranoil SkinRestorer (Forge Mod 1.20.1)
            try {
                Class<?> apiClass = Class.forName("com.suiranoil.skinrestorer.SkinRestorer");
                Method setSkinMethod = apiClass.getMethod("setSkin", UUID.class, String.class);
                setSkinMethod.invoke(null, playerUuid, skinName);
                DebugLogger.log("Skin applied successfully using Suiranoil API.");
                return;
            } catch (ClassNotFoundException e) {
                // Silent
            } catch (Exception e) {
                DebugLogger.error("Suiranoil API failed: " + e.getMessage());
            }

            // Attempt 2: Modern SkinsRestorer (Plugin API)
            try {
                Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
                Method getMethod = providerClass.getMethod("get");
                Object skinsRestorer = getMethod.invoke(null);
                
                Method getSkinApplierMethod = skinsRestorer.getClass().getMethod("getSkinApplier", Class.class);
                Object skinApplier = getSkinApplierMethod.invoke(skinsRestorer, Object.class);
                
                Class<?> skinIdClass = Class.forName("net.skinsrestorer.api.storage.SkinIdentifier");
                Method ofSkinMethod = skinIdClass.getMethod("ofSkin", String.class);
                Object skinId = ofSkinMethod.invoke(null, skinName);
                
                Method applySkinMethod = skinApplier.getClass().getMethod("applySkin", Object.class, skinIdClass);
                applySkinMethod.invoke(skinApplier, playerUuid, skinId);
                DebugLogger.log("Skin applied successfully using SkinsRestorer Modern API.");
                return;
            } catch (ClassNotFoundException e) {
                // Silent
            } catch (Exception e) {
                DebugLogger.error("Modern API failed: " + e.getMessage());
            }

            // Final Fallback: Command via console
            if (server != null) {
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
                String possessorName = (player != null) ? player.getGameProfile().getName() : playerUuid.toString();
                
                // User requirement: /skin set mojang <pseudo du joueur possédé> <possesseur>
                String cmd = String.format("skin set mojang %s %s", skinName, possessorName);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
                DebugLogger.log("Falling back to command: /" + cmd);
            } else {
                DebugLogger.error("SkinRestorer: No suitable API found and server is null for fallback.");
            }
            
        } catch (Exception e) {
            DebugLogger.error("SkinRestorer critical error: " + e.getMessage());
        }
    }

    public static void resetSkin(MinecraftServer server, UUID playerUuid) {
        if (server == null) return;
        try {
            net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null) {
                String name = player.getGameProfile().getName();
                String cmd = String.format("skin set mojang %s %s", name, name);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
                DebugLogger.log("Resetting skin for " + name + ": /" + cmd);
            }
        } catch (Exception e) {
            DebugLogger.error("Failed to reset skin: " + e.getMessage());
        }
    }
}

