package com.example.possessor.client;

import com.example.possessor.game.Role;
import com.example.possessor.game.GamePhase;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class ClientRoleData {
    private static Role myRole = Role.SPECTATOR;
    private static GamePhase currentPhase = GamePhase.LOBBY;
    private static Map<UUID, String> playerSkins = new HashMap<>();

    public static void setRole(Role role) {
        myRole = role;
    }

    public static Role getRole() {
        return myRole;
    }
    
    public static void setPhase(GamePhase phase) {
        currentPhase = phase;
    }
    
    public static GamePhase getPhase() {
        return currentPhase;
    }

    public static void setSkins(Map<UUID, String> skins) {
        playerSkins = skins;
    }

    public static String getSkin(UUID uuid) {
        return playerSkins.getOrDefault(uuid, "???");
    }
}
