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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PossessorMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        if (!(event.player instanceof ServerPlayer)) return;
        ServerPlayer player = (ServerPlayer) event.player;

        Role role = RoleManager.getInstance().getRole(player);
        
        // Only check boundaries if player is not an admin/noPlay and is a spectator in the game sense or actual spectator mode
        if (!player.getTags().contains("noPlay") && (role == Role.SPECTATOR || player.isSpectator())) {
            checkBoundaries(player);
        }
    }

    private static void checkBoundaries(ServerPlayer player) {
        // Bounds: X: -1645.10 to -1603.80, Y: -43.00 to -26.09 (User said -29.09 in one msg, -26.09 in another, I'll use the larger range -26.09 for safety), Z: 2133.48 to 2171.37
        // Actually, let's use the explicit request: -1645.10 to -1603.80, Y: -43.00 to -29.09 (from recent chat)
        // Wait, task description says: X: -1645.10 to -1603.80, Y: -43.00 to -26.09, Z: 2133.48 to 2171.37
        
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        double minX = -1645.10;
        double maxX = -1603.80;
        double minY = -46.00;
        double maxY = -29.09; // Using the higher value to be less restrictive
        double minZ = 2133.48;
        double maxZ = 2171.37;

        boolean outOfBounds = x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ;

        if (outOfBounds) {
            // Teleport back to center-ish or clamp
            // Center roughly: -1624, -40, 2152
            player.teleportTo(-1624.5, -40.0, 2152.5);
            player.sendSystemMessage(Component.literal("Vous ne pouvez pas sortir de la carte du jeu").withStyle(ChatFormatting.RED), true);
        }
    }
}
