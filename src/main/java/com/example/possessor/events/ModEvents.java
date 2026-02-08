package com.example.possessor.events;

import com.example.possessor.PossessorMod;
import com.example.possessor.game.GameManager;
import com.example.possessor.game.GamePhase;
import com.example.possessor.game.Role;
import com.example.possessor.game.RoleManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PossessorMod.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerJoin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        // Nametag hiding via scoreboard removed
    }

    @SubscribeEvent
    public static void onPlayerName(net.minecraftforge.event.entity.player.PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.getTags().contains("noPlay")) return;
            String skinName = GameManager.getInstance().getPlayerSkinName(player.getUUID());
            if (skinName != null && GameManager.getInstance().getCurrentPhase() != GamePhase.LOBBY) {
                event.setDisplayname(Component.literal(skinName));
            }
        }
    }

    @SubscribeEvent
    public static void onTabListName(net.minecraftforge.event.entity.player.PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() != null && event.getEntity().getTags().contains("noPlay")) return;
        if (GameManager.getInstance().getCurrentPhase() != GamePhase.LOBBY) {
            // Using a fixed length of obfuscated characters to prevent identification by name length
            event.setDisplayName(Component.literal("||||||||||").withStyle(ChatFormatting.OBFUSCATED));
        }
    }

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
             net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
             if (server != null) {
                 GameManager.getInstance().tick(server);
                 
                  // Spectator bound restriction
                  for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                      if (player.isSpectator()) {
                          double x = player.getX();
                          double y = player.getY();
                          double z = player.getZ();
                          
                          // Bounds: X: -1645.10 to -1603.80, Y: -46.00 to -29.09, Z: 2133.48 to 2171.37
                          boolean outX = x < -1645.10 || x > -1603.80;
                          boolean outY = y < -49.00 || y > -29.09; // -46.00 - 3 = -49.00
                          boolean outZ = z < 2133.48 || z > 2171.37;
                          
                           if (outX || outY || outZ) {
                               player.teleportTo(-1624.0, -43.0, 2152.0); // Center of the area
                               player.sendSystemMessage(Component.literal("Vous ne pouvez pas sortir de la carte du jeu").withStyle(ChatFormatting.RED));
                           }
                      }
                  }
             }
        }
    }

    @SubscribeEvent
    public static void onEntityMount(net.minecraftforge.event.entity.EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityMounting() instanceof ServerPlayer player) {
            GamePhase phase = GameManager.getInstance().getCurrentPhase();
            
            // Prevent dismount if:
            // 1. Phase is REVEAL (Everyone stays on their seat)
            // 2. Phase is SELECTION (Everyone waits)
            // 3. Phase is VOTING
            
            if (phase == GamePhase.REVEAL || phase == GamePhase.SELECTION || phase == GamePhase.VOTING) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getTags().contains("noPlay")) return;
        
        GameManager gm = GameManager.getInstance();
        if (gm.getCurrentPhase() != GamePhase.PLAYING && gm.getCurrentPhase() != GamePhase.VOTING) return;

        Role role = RoleManager.getInstance().getRole(player);
        if (role == Role.POSSESSOR) {
            // Possessor died, Innocents win
            gm.stopGame(player.server);
            player.server.getPlayerList().broadcastSystemMessage(Component.literal("LE POSSESSEUR A ÉTÉ ÉLIMINÉ ! LES INNOCENTS GAGNENT !").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        } else if (role == Role.INNOCENT) {
            // Innocent died
            player.stopRiding(); // Free from chair if dead
            RoleManager.getInstance().setRole(player, Role.SPECTATOR);
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR); 
            player.sendSystemMessage(Component.literal("Vous êtes mort et maintenant spectateur.").withStyle(ChatFormatting.GRAY));
            
            // Teleport to possessor once
            ServerPlayer possessor = null;
            for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                if (RoleManager.getInstance().getRole(p) == Role.POSSESSOR) {
                    possessor = p;
                    break;
                }
            }
            if (possessor != null) {
                player.teleportTo(possessor.getX(), possessor.getY(), possessor.getZ());
            }
            
            checkWinCondition(player.server);
        }
    }

    private static void checkWinCondition(net.minecraft.server.MinecraftServer server) {
        long innocentCount = server.getPlayerList().getPlayers().stream()
                .filter(p -> RoleManager.getInstance().getRole(p) == Role.INNOCENT)
                .count();

        if (innocentCount == 0) {
            GameManager.getInstance().stopGame(server);
            server.getPlayerList().broadcastSystemMessage(Component.literal("TOUS LES INNOCENTS SONT ÉLIMINÉS ! LE POSSESSEUR GAGNE !").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer source) {
            if (source.getTags().contains("noPlay") || target.getTags().contains("noPlay")) return;
            if (source.isSpectator() || RoleManager.getInstance().getRole(source) == Role.SPECTATOR) {
                event.setCanceled(true);
                return;
            }

            GamePhase phase = GameManager.getInstance().getCurrentPhase();
            if (phase == GamePhase.SELECTION || phase == GamePhase.VOTING) {
                // Let the packet or Attack event handle it, just cancel default interaction
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer source) {
            if (source.getTags().contains("noPlay") || target.getTags().contains("noPlay")) return;
            if (source.isSpectator() || RoleManager.getInstance().getRole(source) == Role.SPECTATOR) {
                event.setCanceled(true);
                return;
            }

            GamePhase phase = GameManager.getInstance().getCurrentPhase();
            if (phase == GamePhase.SELECTION && RoleManager.getInstance().getRole(source) == Role.POSSESSOR) {
                GameManager.getInstance().setPendingTarget(target.getUUID());
                source.sendSystemMessage(Component.literal("Cible sélectionnée : " + target.getName().getString()).withStyle(ChatFormatting.RED));
                event.setCanceled(true);
            } else if (phase == GamePhase.VOTING) {
                com.example.possessor.game.VoteManager.getInstance().castVote(source.getUUID(), target.getUUID());
                source.sendSystemMessage(Component.literal("A voté pour " + target.getName().getString()).withStyle(ChatFormatting.GREEN));
                GameManager.getInstance().syncVotes(source.server);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        if (event.getPlayer() != null && event.getPlayer().getTags().contains("noPlay")) return;
        GamePhase phase = GameManager.getInstance().getCurrentPhase();
        if (phase == GamePhase.PLAYING) {
             ServerPlayer sender = event.getPlayer();
             if (sender == null) return;
             
             event.setCanceled(true);
             
             Component message = Component.literal("<" + sender.getName().getString() + "> ").withStyle(ChatFormatting.GRAY)
                 .append(event.getMessage());
                 
             for (ServerPlayer receiver : sender.server.getPlayerList().getPlayers()) {
                 if (receiver.distanceToSqr(sender) <= 225.0) {
                     receiver.sendSystemMessage(message);
                 }
             }
        }
    }
}
