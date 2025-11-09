package com.fox.ysmu.client.gui.button;

import com.fox.ysmu.ysmu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;

public class FlatIconButton extends FlatColorButton {
    private final static ResourceLocation ICON = new ResourceLocation(ysmu.MODID, "texture/icon.png");
    private final int textureX;
    private final int textureY;

    public FlatIconButton(int x, int y, int width, int height, int textureX, int textureY, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress);
        this.textureX = textureX;
        this.textureY = textureY;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float pPartialTick) {
        super.renderWidget(graphics, mouseX, mouseY, pPartialTick);
        int startX = (this.width - 16) / 2;
        int startY = (this.height - 16) / 2;
        graphics.blit(ICON, this.getX() + startX, this.getY() + startY, 16, 16, textureX, textureY, 16, 16, 256, 256);
    }
}
