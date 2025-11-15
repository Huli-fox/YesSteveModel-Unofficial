package com.fox.ysmu.client.gui.button;

import com.fox.ysmu.ysmu;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class FlatIconButton extends FlatColorButton {
    private final static ResourceLocation ICON = new ResourceLocation(ysmu.MODID, "texture/icon.png");
    private final int textureX;
    private final int textureY;

    public FlatIconButton(int id, int x, int y, int width, int height, int textureX, int textureY) {
        super(id, x, y, width, height, "");
        this.textureX = textureX;
        this.textureY = textureY;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        super.drawButton(mc, mouseX, mouseY);
        int startX = (this.width - 16) / 2;
        int startY = (this.height - 16) / 2;
        mc.getTextureManager().bindTexture(ICON);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawTexturedModalRect(this.xPosition + startX, this.yPosition + startY, textureX, textureY, 16, 16);
    }
}
