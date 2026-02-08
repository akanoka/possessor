package com.example.possessor.network;

import com.example.possessor.game.GameManager;
import com.example.possessor.game.GamePhase;
import com.example.possessor.game.Role;
import com.example.possessor.game.RoleManager;
import com.example.possessor.game.VoteManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketSelectPlayer {
    private final UUID targetUuid;

    public PacketSelectPlayer(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public PacketSelectPlayer(FriendlyByteBuf buf) {
        this.targetUuid = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(targetUuid);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            GameManager gm = GameManager.getInstance();
            GamePhase phase = gm.getCurrentPhase();
            Role role = RoleManager.getInstance().getRole(player);

            // Strict block for spectators
            if (player.isSpectator() || role == Role.SPECTATOR) {
                return;
            }

            if (phase == GamePhase.SELECTION && role == Role.POSSESSOR) {
                gm.setPendingTarget(targetUuid);
                ServerPlayer target = player.server.getPlayerList().getPlayer(targetUuid);
                if (target != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Cible sélectionnée : " + target.getName().getString()).withStyle(net.minecraft.ChatFormatting.RED));
                }
            } else if (phase == GamePhase.VOTING) {
                VoteManager.getInstance().castVote(player.getUUID(), targetUuid);
                ServerPlayer target = player.server.getPlayerList().getPlayer(targetUuid);
                if (target != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("A voté pour " + target.getName().getString()).withStyle(net.minecraft.ChatFormatting.GREEN));
                }
                gm.syncVotes(player.server);
            }
        });
        context.setPacketHandled(true);
    }
}
