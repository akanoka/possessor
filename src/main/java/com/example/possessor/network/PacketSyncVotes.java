package com.example.possessor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class PacketSyncVotes {
    // Target -> List of Voters
    private final Map<UUID, List<UUID>> votes;

    public PacketSyncVotes(Map<UUID, List<UUID>> votes) {
        this.votes = votes;
    }

    public PacketSyncVotes(FriendlyByteBuf buffer) {
        votes = new HashMap<>();
        int targetCount = buffer.readInt();
        for (int i = 0; i < targetCount; i++) {
            UUID target = buffer.readUUID();
            int voterCount = buffer.readInt();
            List<UUID> voters = new ArrayList<>();
            for (int j = 0; j < voterCount; j++) {
                voters.add(buffer.readUUID());
            }
            votes.put(target, voters);
        }
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeInt(votes.size());
        for (Map.Entry<UUID, List<UUID>> entry : votes.entrySet()) {
            buffer.writeUUID(entry.getKey());
            buffer.writeInt(entry.getValue().size());
            for (UUID voter : entry.getValue()) {
                buffer.writeUUID(voter);
            }
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client side handling
            // We need a ClientVoteHandler class
            com.example.possessor.client.ClientVoteHandler.updateVotes(votes);
        });
        return true;
    }
}
