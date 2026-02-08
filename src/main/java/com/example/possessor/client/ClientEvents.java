package com.example.possessor.client;

import com.example.possessor.PossessorMod;
import com.example.possessor.game.GamePhase;
import com.example.possessor.network.PacketHandler;
import com.example.possessor.network.PacketSelectPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PossessorMod.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientInteract(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getLevel().isClientSide) trySelectPlayer();
    }

    @SubscribeEvent
    public static void onClientLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getLevel().isClientSide) trySelectPlayer();
    }

    @SubscribeEvent
    public static void onClientInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) trySelectPlayer();
    }
    
    @SubscribeEvent
    public static void onClientAttackEntity(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide) {
             trySelectPlayer();
        }
    }

    @SubscribeEvent
    public static void onClientInteractBlock(PlayerInteractEvent.RightClickBlock event) {
        // Blocks can be in between, let's also try here
        if (event.getLevel().isClientSide) {
             trySelectPlayer();
        }
    }
    
    @SubscribeEvent
    public static void onClientInteractItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide) {
            trySelectPlayer();
        }
    }

    private static void trySelectPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GamePhase phase = ClientRoleData.getPhase();
        if (phase != GamePhase.SELECTION && phase != GamePhase.VOTING) return;

        // Message to debug interaction if needed
        // mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Checking selection..."));

        // Perform a long range raycast (e.g. 50 blocks)
        double reach = 50.0;
        HitResult hit = mc.player.pick(reach, 0.0f, false);
        
        // Minecraft's pick() only does blocks. We need entity pick.
        Entity camera = mc.getCameraEntity();
        if (camera != null) {
            EntityHitResult entityHit = projectEntityHit(camera, reach);

            if (entityHit != null && entityHit.getEntity() instanceof Player target) {
                PacketHandler.INSTANCE.sendToServer(new PacketSelectPlayer(target.getUUID()));
            }
        }
    }

    private static EntityHitResult projectEntityHit(Entity shooter, double reach) {
        net.minecraft.world.phys.Vec3 eyePos = shooter.getEyePosition(1.0f);
        net.minecraft.world.phys.Vec3 viewVec = shooter.getViewVector(1.0f);
        net.minecraft.world.phys.Vec3 endPos = eyePos.add(viewVec.x * reach, viewVec.y * reach, viewVec.z * reach);
        net.minecraft.world.phys.AABB searchBox = shooter.getBoundingBox().expandTowards(viewVec.scale(reach)).inflate(1.0D, 1.0D, 1.0D);

        for (Entity target : shooter.level().getEntities(shooter, searchBox, e -> e instanceof Player)) {
            net.minecraft.world.phys.AABB targetBox = target.getBoundingBox().inflate(target.getPickRadius());
            java.util.Optional<net.minecraft.world.phys.Vec3> clip = targetBox.clip(eyePos, endPos);
            if (clip.isPresent()) {
                return new EntityHitResult(target, clip.get());
            }
        }
        return null;
    }
}
