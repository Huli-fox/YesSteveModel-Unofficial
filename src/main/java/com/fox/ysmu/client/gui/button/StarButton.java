package com.fox.ysmu.client.gui.button;

import com.fox.ysmu.ysmu;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.eep.ExtendedStarModels;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.SetStarModel;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class StarButton extends FlatColorButton {
    private final static ResourceLocation ICON = new ResourceLocation(ysmu.MODID, "texture/icon.png");

    public StarButton(int id, int x, int y) {
        super(id, x, y, 20, 20, "");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        super.drawButton(mc, mouseX, mouseY);
        int startX = (this.width - 16) / 2;
        int startY = (this.height - 16) / 2;
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            ExtendedModelInfo modelInfoEEP = ExtendedModelInfo.get(player);
            ExtendedStarModels starModelsEEP = ExtendedStarModels.get(player);
            if (modelInfoEEP != null && starModelsEEP != null) {
                ResourceLocation modelId = modelInfoEEP.getModelId();
                if (starModelsEEP.containModel(modelId)) {
                    mc.getTextureManager().bindTexture(ICON);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    this.drawTexturedModalRect(this.xPosition + startX, this.yPosition + startY, 16, 0, 16, 16);
                } else {
                    mc.getTextureManager().bindTexture(ICON);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    this.drawTexturedModalRect(this.xPosition + startX, this.yPosition + startY, 0, 0, 16, 16);
                }
            }
        }
    }

    public void doPress() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            ExtendedModelInfo modelInfoEEP = ExtendedModelInfo.get(player);
            ExtendedStarModels starModelsEEP = ExtendedStarModels.get(player);
            if (modelInfoEEP != null && starModelsEEP != null) {
                ResourceLocation modelId = modelInfoEEP.getModelId();
                if (starModelsEEP.containModel(modelId)) {
                    starModelsEEP.removeModel(modelId);
                    NetworkHandler.CHANNEL.sendToServer(SetStarModel.remove(modelId));
                } else {
                    starModelsEEP.addModel(modelId);
                    NetworkHandler.CHANNEL.sendToServer(SetStarModel.add(modelId));
                }
            }
        }
    }
}
