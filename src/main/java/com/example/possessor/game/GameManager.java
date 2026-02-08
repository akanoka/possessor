package com.example.possessor.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GameManager {
    private static final GameManager INSTANCE = new GameManager();
    private GamePhase currentPhase = GamePhase.LOBBY;
    private int phaseTimer = 0; // Ticks
    private final Map<UUID, String> playerSkinNames = new HashMap<>();
    private final List<ChairLocation> chairLocations = new ArrayList<>();
    private final List<UUID> seatEntityIds = new ArrayList<>();

    private static class ChairLocation {
        final Vec3 pos;
        final float yaw;

        ChairLocation(double x, double y, double z, float yaw) {
            this.pos = new Vec3(x, y, z);
            this.yaw = yaw;
        }
    }

    private GameManager() {
        chairLocations.add(new ChairLocation(-1626, -44, 2148, 0));
        chairLocations.add(new ChairLocation(-1625, -44, 2148, 0));
        chairLocations.add(new ChairLocation(-1623, -44, 2149, 45));
        chairLocations.add(new ChairLocation(-1622, -44, 2151, 90));
        chairLocations.add(new ChairLocation(-1622, -44, 2152, 90));
        chairLocations.add(new ChairLocation(-1623, -44, 2154, 135));
        chairLocations.add(new ChairLocation(-1625, -44, 2155, 180));
        chairLocations.add(new ChairLocation(-1626, -44, 2155, 180));
        chairLocations.add(new ChairLocation(-1628, -44, 2154, -45));
        chairLocations.add(new ChairLocation(-1629, -44, 2152, -90));
        chairLocations.add(new ChairLocation(-1629, -44, 2151, -90));
        chairLocations.add(new ChairLocation(-1628, -44, 2149, -135));
    }

    public static GameManager getInstance() {
        return INSTANCE;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public void tick(MinecraftServer server) {
        if (currentPhase == GamePhase.LOBBY || currentPhase == GamePhase.ENDING) return;

        phaseTimer--;

        if (phaseTimer % 20 == 0 && phaseTimer > 0) {
            String timeStr = formatTime(phaseTimer / 20);
            server.getPlayerList().broadcastSystemMessage(Component.literal("Temps restant : " + timeStr).withStyle(ChatFormatting.YELLOW), true);
        }

        if (phaseTimer <= 0) {
            advancePhase(server);
        }
        
        if (currentPhase == GamePhase.PLAYING || currentPhase == GamePhase.VOTING || currentPhase == GamePhase.SELECTION || currentPhase == GamePhase.REVEAL) {
             List<ServerPlayer> active = server.getPlayerList().getPlayers().stream()
                 .filter(p -> !p.isSpectator() && RoleManager.getInstance().getRole(p) != Role.SPECTATOR && !p.getTags().contains("noPlay"))
                 .collect(Collectors.toList());
             
             if (active.size() <= 2 && currentPhase != GamePhase.ENDING) {
                 stopGame(server);
                 server.getPlayerList().broadcastSystemMessage(Component.literal("Fin de partie : Il ne reste que 2 joueurs !").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
             }
        }
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    public void startGame(MinecraftServer server) {
        if (currentPhase != GamePhase.LOBBY) return;

        List<ServerPlayer> players = server.getPlayerList().getPlayers().stream()
            .filter(p -> !p.getTags().contains("noPlay"))
            .collect(Collectors.toList());
        if (players.size() > 12) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("Trop de joueurs ! Max 12.").withStyle(ChatFormatting.RED), false);
            return;
        }

        RoleManager.getInstance().clearRoles();
        List<ServerPlayer> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);

        ServerPlayer possessor = shuffledPlayers.get(0);
        RoleManager.getInstance().setRole(possessor, Role.POSSESSOR);
        
        for (int i = 1; i < shuffledPlayers.size(); i++) {
            RoleManager.getInstance().setRole(shuffledPlayers.get(i), Role.INNOCENT);
        }

        playerSkinNames.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getTags().contains("noPlay")) continue;
            playerSkinNames.put(player.getUUID(), player.getGameProfile().getName());
        }
        updateNametags(server);
        startRevealPhase(server, shuffledPlayers);
        syncState(server);
    }

    private void applySeating(MinecraftServer server, List<ServerPlayer> players) {
        int seatIndex = 0;
        ServerLevel level = server.overworld();
        cleanupSeats(server);

        for (ServerPlayer player : players) {
            if (seatIndex >= chairLocations.size()) break;
            ChairLocation loc = chairLocations.get(seatIndex++);
            Vec3 coord = loc.pos;
            
            ArmorStand seat = new ArmorStand(level, coord.x + 0.5, coord.y - 0.5, coord.z + 0.5);
            seat.setYRot(loc.yaw);
            seat.setYHeadRot(loc.yaw);
            seat.setInvisible(true);
            seat.setNoGravity(true);
            CompoundTag nbt = new CompoundTag();
            seat.addAdditionalSaveData(nbt);
            nbt.putBoolean("Small", true);
            seat.readAdditionalSaveData(nbt);
            seat.setShowArms(false);
            seat.setNoBasePlate(true);
            level.addFreshEntity(seat);
            seatEntityIds.add(seat.getUUID());

            player.teleportTo(level, coord.x + 0.5, coord.y - 0.5, coord.z + 0.5, loc.yaw, 0);
            player.startRiding(seat, true);
        }
    }

    private void startRevealPhase(MinecraftServer server, List<ServerPlayer> players) {
        currentPhase = GamePhase.REVEAL;
        phaseTimer = 15 * 20;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                Component.literal("RÉVÉLATION").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            ));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                Component.literal("Attribution des rôles...").withStyle(ChatFormatting.YELLOW)
            ));
            player.playNotifySound(net.minecraft.sounds.SoundEvents.ILLUSIONER_PREPARE_MIRROR, net.minecraft.sounds.SoundSource.MASTER, 1.0f, 1.0f);
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal("=== PHASE 1 : RÉVÉLATION ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        applySeating(server, players);

        for (ServerPlayer player : players) {
            Role role = RoleManager.getInstance().getRole(player);
            if (role == Role.POSSESSOR) {
                player.sendSystemMessage(Component.literal("VOUS ÊTES LE POSSESSEUR !").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 15 * 20, 0, false, false));
            } else {
                player.sendSystemMessage(Component.literal("VOUS ÊTES INNOCENT.").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            }
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                 Component.literal("Vous êtes : " + role.name()).withStyle(role == Role.POSSESSOR ? ChatFormatting.RED : ChatFormatting.GREEN)
            ));
        }
    }

    private void cleanupSeats(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (UUID uuid : seatEntityIds) {
            var entity = level.getEntity(uuid);
            if (entity != null) entity.discard();
        }
        seatEntityIds.clear();
    }

    public void skipPhase() {
        if (currentPhase != GamePhase.LOBBY) {
            phaseTimer = 0;
            DebugLogger.log("Phase skipped by admin.");
        }
    }

    private void advancePhase(MinecraftServer server) {
        switch (currentPhase) {
            case REVEAL: startSelectionPhase(server); break;
            case SELECTION: startPlayingPhase(server); break;
            case PLAYING: startVotingPhase(server); break;
            case VOTING: finishVotingPhase(server); break;
            default: stopGame(server); break;
        }
        syncState(server);
    }

    private void startSelectionPhase(MinecraftServer server) {
        currentPhase = GamePhase.SELECTION;
        phaseTimer = 20 * 20;
        server.getPlayerList().broadcastSystemMessage(Component.literal("=== PHASE 2 : SÉLECTION ===").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD), false);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                Component.literal("SÉLECTION").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD)
            ));
            player.removeEffect(MobEffects.LEVITATION);
        }
    }

    private UUID pendingTarget = null;
    public void setPendingTarget(UUID targetId) { this.pendingTarget = targetId; }
    public String getPlayerSkinName(UUID uuid) { return playerSkinNames.get(uuid); }

    private void startPlayingPhase(MinecraftServer server) {
        currentPhase = GamePhase.PLAYING;
        phaseTimer = 150 * 20;
        server.getPlayerList().broadcastSystemMessage(Component.literal("=== PHASE 3 : JEU ===").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getVehicle() != null) {
                player.stopRiding();
                player.teleportTo(player.getX(), player.getY() + 1.0, player.getZ());
            }
        }
        cleanupSeats(server);
        
        if (pendingTarget == null) {
            List<ServerPlayer> targets = server.getPlayerList().getPlayers().stream()
                .filter(p -> RoleManager.getInstance().getRole(p) == Role.INNOCENT && !p.getTags().contains("noPlay"))
                .collect(Collectors.toList());
            if (!targets.isEmpty()) {
                Collections.shuffle(targets);
                pendingTarget = targets.get(0).getUUID();
                DebugLogger.log("No target chosen, picking random: " + targets.get(0).getName().getString());
            }
        }
        
        if (pendingTarget != null) {
            ServerPlayer victim = server.getPlayerList().getPlayer(pendingTarget);
            if (victim != null && RoleManager.getInstance().getRole(victim) != Role.POSSESSOR) {
                ServerPlayer possessor = null;
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (RoleManager.getInstance().getRole(p) == Role.POSSESSOR) {
                        possessor = p;
                        break;
                    }
                }
                if (possessor != null) {
                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
                    }
                    possessor.teleportTo(victim.getX(), victim.getY(), victim.getZ());
                    RoleManager.getInstance().setRole(victim, Role.SPECTATOR);
                    victim.setGameMode(GameType.SPECTATOR);
                    victim.teleportTo(possessor.getX(), possessor.getY(), possessor.getZ());
                    String victimName = victim.getName().getString();
                    com.example.possessor.compat.SkinRestorerCompat.setSkin(server, possessor.getUUID(), victimName);
                    playerSkinNames.put(possessor.getUUID(), victimName);
                    updateNametags(server);
                }
            }
            pendingTarget = null;
        }
    }

    private void startVotingPhase(MinecraftServer server) {
        currentPhase = GamePhase.VOTING;
        phaseTimer = 90 * 20;
        server.getPlayerList().broadcastSystemMessage(Component.literal("=== PHASE 4 : VOTE ===").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
        List<ServerPlayer> activePlayers = server.getPlayerList().getPlayers().stream()
            .filter(p -> !p.isSpectator() && RoleManager.getInstance().getRole(p) != Role.SPECTATOR)
            .collect(Collectors.toList());
        applySeating(server, activePlayers);
        VoteManager.getInstance().clearVotes();
        syncVotes(server);
    }

    private void finishVotingPhase(MinecraftServer server) {
        Map<UUID, List<UUID>> summary = VoteManager.getInstance().getVoteSummary();
        UUID eliminated = null; int maxVotes = -1; boolean tie = false;
        for (Map.Entry<UUID, List<UUID>> entry : summary.entrySet()) {
            if (entry.getValue().size() > maxVotes) { maxVotes = entry.getValue().size(); eliminated = entry.getKey(); tie = false; }
            else if (entry.getValue().size() == maxVotes) { tie = true; }
        }

        if (eliminated != null && !tie) {
            ServerPlayer victim = server.getPlayerList().getPlayer(eliminated);
            if (victim != null) {
                String skinName = playerSkinNames.getOrDefault(eliminated, victim.getName().getString());
                server.getPlayerList().broadcastSystemMessage(Component.literal(skinName + " a été éliminé par le vote !").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
                victim.kill();
            }
        } else {
            server.getPlayerList().broadcastSystemMessage(Component.literal("Égalité ou aucun vote !").withStyle(ChatFormatting.YELLOW), false);
        }
        VoteManager.getInstance().clearVotes();
        syncVotes(server);
        if (currentPhase != GamePhase.LOBBY) startSelectionPhase(server);
    }

    public void stopGame(MinecraftServer server) {
        if (currentPhase == GamePhase.LOBBY) return;
        
        String revealName = "Inconnu";
        for (UUID uuid : playerSkinNames.keySet()) {
            if (RoleManager.getInstance().getRole(uuid) == Role.POSSESSOR) {
                revealName = playerSkinNames.get(uuid);
                break;
            }
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal("La partie est terminée !").withStyle(ChatFormatting.GOLD), false);
        server.getPlayerList().broadcastSystemMessage(Component.literal("Le Possesseur était déguisé en : " + revealName).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getTags().contains("noPlay")) continue;
            player.teleportTo(player.server.overworld(), -1625.03, -44.00, 2151.72, player.getYRot(), player.getXRot());
            com.example.possessor.compat.SkinRestorerCompat.resetSkin(server, player.getUUID());
            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(20.0f);
        }
        cleanupSeats(server);
        currentPhase = GamePhase.LOBBY;
        playerSkinNames.clear();
        updateNametags(server);
        syncState(server);
    }

    public void updateNametags(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getTags().contains("noPlay")) continue;
            if (currentPhase != GamePhase.LOBBY) {
                String skinName = playerSkinNames.get(player.getUUID());
                if (skinName != null) {
                    player.setCustomName(Component.literal(skinName));
                    player.setCustomNameVisible(true);
                } else {
                    player.setCustomName(null);
                    player.setCustomNameVisible(true);
                }
            } else {
                player.setCustomName(null);
                player.setCustomNameVisible(true);
            }
        }
    }

    public void syncVotes(MinecraftServer server) {
        com.example.possessor.network.PacketHandler.INSTANCE.send(
            net.minecraftforge.network.PacketDistributor.ALL.noArg(),
            new com.example.possessor.network.PacketSyncVotes(VoteManager.getInstance().getVoteSummary())
        );
    }

    public void syncState(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
             Role role = RoleManager.getInstance().getRole(player);
             com.example.possessor.network.PacketHandler.INSTANCE.send(
                 net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                 new com.example.possessor.network.PacketSyncData(role, currentPhase, new HashMap<>(playerSkinNames))
             );
        }
    }
}
