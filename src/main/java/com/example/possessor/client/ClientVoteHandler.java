package com.example.possessor.client;

import java.util.*;

public class ClientVoteHandler {
    private static Map<UUID, List<UUID>> currentVotes = new HashMap<>();

    public static void updateVotes(Map<UUID, List<UUID>> votes) {
        currentVotes = votes;
    }

    public static List<UUID> getVotersFor(UUID target) {
        return currentVotes.getOrDefault(target, Collections.emptyList());
    }

    public static int getVoteCount(UUID target) {
        return getVotersFor(target).size();
    }
    
    public static void clear() {
        currentVotes.clear();
    }
}
