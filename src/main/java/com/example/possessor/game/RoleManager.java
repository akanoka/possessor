package com.example.possessor.game;

import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoleManager {
    private static final RoleManager INSTANCE = new RoleManager();
    private final Map<UUID, Role> playerRoles = new HashMap<>();

    private RoleManager() {}

    public static RoleManager getInstance() {
        return INSTANCE;
    }

    public void setRole(ServerPlayer player, Role role) {
        playerRoles.put(player.getUUID(), role);
    }

    public Role getRole(ServerPlayer player) {
        return getRole(player.getUUID());
    }

    public Role getRole(UUID uuid) {
        return playerRoles.getOrDefault(uuid, Role.SPECTATOR); // Default to spectator if not found
    }

    private final Map<UUID, UUID> possessedSouls = new HashMap<>(); // Victim -> Possessor

    public void setSoul(UUID victim, UUID possessor) {
        possessedSouls.put(victim, possessor);
    }

    public UUID getPossessorOfSoul(UUID victim) {
        return possessedSouls.get(victim);
    }

    public Map<UUID, UUID> getPossessedSouls() {
        return possessedSouls;
    }

    public void clearRoles() {
        playerRoles.clear();
        possessedSouls.clear();
    }
}
