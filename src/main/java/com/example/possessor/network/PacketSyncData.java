package com.example.possessor.network;

import com.example.possessor.game.GamePhase;
import com.example.possessor.game.Role;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Supplier;

public class PacketSyncData {
    private final Role role;
    private final GamePhase phase;
    private final Map<UUID, String> skins;

    public PacketSyncData(Role role, GamePhase phase, Map<UUID, String> skins) {
        this.role = role;
        this.phase = phase;
        this.skins = skins;
    }

    public PacketSyncData(FriendlyByteBuf buf) {
        this.role = buf.readEnum(Role.class);
        this.phase = buf.readEnum(GamePhase.class);
        this.skins = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            skins.put(buf.readUUID(), buf.readUtf());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(role);
        buf.writeEnum(phase);
        buf.writeInt(skins.size());
        skins.forEach((uuid, skin) -> {
            buf.writeUUID(uuid);
            buf.writeUtf(skin);
        });
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client side handling
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.example.possessor.client.ClientRoleData.setRole(role);
                com.example.possessor.client.ClientRoleData.setPhase(phase);
                com.example.possessor.client.ClientRoleData.setSkins(skins);
            });
        });
        return true;
    }
}
