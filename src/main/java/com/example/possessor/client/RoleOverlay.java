package com.example.possessor.client;

import com.example.possessor.game.Role;
import com.example.possessor.game.RoleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class RoleOverlay implements IGuiOverlay {
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // AGAIN: RoleManager is server-side map. Client doesn't know its role unless we sync it.
        // We need a ClientRoleHandler.
        
        // Let's assume we read from a Client-side static holder we will populate via packets.
        Role role = ClientRoleData.getRole();
        
        if (role == null || role == Role.SPECTATOR) return; // Maybe show spectator?
        
        String text = "Role: " + role.name();
        int color = (role == Role.POSSESSOR) ? 0xFF0000 : 0x00FF00;
        
        Font font = mc.font;
        int x = 10;
        int y = screenHeight - 20;
        
        guiGraphics.drawString(font, Component.literal(text), x, y, color);
    }
}
