package com.fox.ysmu.client.gui;

import com.fox.ysmu.Config;
import com.fox.ysmu.util.Keep;
import com.fox.ysmu.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class ExtraPlayerScreen implements IGuiOverlay {
    @Override
    @Keep
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (Config.DISABLE_PLAYER_RENDER) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        if (mc.screen instanceof ExtraPlayerConfigScreen) {
            return;
        }

        double posX = Config.PLAYER_POS_X;
        double posY = Config.PLAYER_POS_Y;
        float scale = (float) Config.PLAYER_SCALE;
        float yawOffset = (float) Config.PLAYER_YAW_OFFSET;

        RenderUtil.renderPlayerEntity(player, posX, posY, scale, yawOffset, -500);
    }
}