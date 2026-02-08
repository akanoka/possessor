package com.example.possessor.client;

import com.example.possessor.PossessorMod;
import com.example.possessor.game.GamePhase;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = PossessorMod.MODID, value = Dist.CLIENT)
public class ClientVoteRenderer {

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        GamePhase phase = ClientRoleData.getPhase();
        if (phase == GamePhase.LOBBY) return;

        renderOverheadInfo(event.getPoseStack(), event.getMultiBufferSource(), player);
    }

    private static void renderOverheadInfo(PoseStack poseStack, MultiBufferSource buffer, Player player) {
        int votes = ClientVoteHandler.getVoteCount(player.getUUID());
        List<UUID> voters = ClientVoteHandler.getVotersFor(player.getUUID());
        String skinName = ClientRoleData.getSkin(player.getUUID());

        if (skinName == null && votes == 0) return;

        float baseOffset = player.getBbHeight() + 0.5F;
        
        poseStack.pushPose();
        poseStack.translate(0.0D, baseOffset, 0.0D); 
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        Font font = Minecraft.getInstance().font;
        float yOffset = 0;

        // Render Skin Name
        if (skinName != null) {
            float skinX = (float)(-font.width(skinName) / 2);
            font.drawInBatch(skinName, skinX, yOffset, 0x00FFFF, true, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 15728880);
            yOffset -= 10;
        }

        // Render Vote Count
        if (votes > 0) {
            String voteText = "Votes : " + votes;
            float voteX = (float)(-font.width(voteText) / 2);
            font.drawInBatch(voteText, voteX, yOffset, -1, true, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 15728880);
            yOffset -= 10;
        }

        // Render Voters
        int i = 0;
        for (UUID voterId : voters) {
            String name = ClientRoleData.getSkin(voterId);
            if (name == null) continue;
            float voterX = (float)(-font.width(name) / 2);
            font.drawInBatch(name, voterX, yOffset - (i * 10), 0x00FFFF, true, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 15728880);
            i++;
        }

        poseStack.popPose();
    }
}
