package com.example.possessor.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class VoteManager {
    private static final VoteManager INSTANCE = new VoteManager();
    // Map<VoterUUID, TargetUUID>
    private final Map<UUID, UUID> votes = new HashMap<>();

    private VoteManager() {}

    public static VoteManager getInstance() {
        return INSTANCE;
    }

    public void clearVotes() {
        votes.clear();
    }

    public void castVote(UUID voter, UUID target) {
        votes.put(voter, target);
    }

    public UUID getVote(UUID voter) {
        return votes.get(voter);
    }

    public Map<UUID, List<UUID>> getVoteSummary() {
        // Target -> List of Voters
        Map<UUID, List<UUID>> summary = new HashMap<>();
        for (Map.Entry<UUID, UUID> entry : votes.entrySet()) {
            summary.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        return summary;
    }

    public void resolveVotes(MinecraftServer server) {
        // Find who has the most votes
        Map<UUID, Integer> counts = new HashMap<>();
        for (UUID target : votes.values()) {
            counts.put(target, counts.getOrDefault(target, 0) + 1);
        }

        // Simple majority logic
        UUID eliminated = null;
        int maxVotes = -1;
        boolean tie = false;

        for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                eliminated = entry.getKey();
                tie = false;
            } else if (entry.getValue() == maxVotes) {
                tie = true;
            }
        }

        if (eliminated != null && !tie) {
            ServerPlayer player = server.getPlayerList().getPlayer(eliminated);
            if (player != null) {
                player.kill(); // Trigger death event which handles logic
            }
        } else {
             // Tie or no votes - maybe nothing happens? or random?
             // Prompt says "si c'était le possesseur... fin de game... si c'était pas... le joueur éliminé meurt"
             // Implies someone MUST be eliminated? Or if tie, nobody dies?
             // I'll assume tie = nobody dies.
        }
    }
}
