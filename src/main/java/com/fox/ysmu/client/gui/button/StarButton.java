package com.fox.ysmu.client.gui.button;

import com.fox.ysmu.ysmu;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.eep.ExtendedStarModels;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.SetStarModel;
import com.fox.ysmu.util.Keep;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;

public class StarButton extends FlatColorButton {
    private final static ResourceLocation ICON = new ResourceLocation(ysmu.MODID, "texture/icon.png");

    public StarButton(int x, int y) {
        super(x, y, 20, 20, Component.empty(), (b) -> {
        });
    }

    @Override
    @Keep
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float pPartialTick) {
        super.renderWidget(graphics, mouseX, mouseY, pPartialTick);
        int startX = (this.width - 16) / 2;
        int startY = (this.height - 16) / 2;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ExtendedModelInfo modelInfoEEP = ExtendedModelInfo.get(player);
            ExtendedStarModels starModelsEEP = ExtendedStarModels.get(player);
            if (modelInfoEEP != null && starModelsEEP != null) {
                ResourceLocation modelId = modelInfoEEP.getModelId();
                if (starModelsEEP.containModel(modelId)) {
                    graphics.blit(ICON, this.getX() + startX, this.getY() + startY, 16, 16, 16, 0, 16, 16, 256, 256);
                } else {
                    graphics.blit(ICON, this.getX() + startX, this.getY() + startY, 16, 16, 0, 0, 16, 16, 256, 256);
                }
            }
        }
    }

    @Override
    @Keep
    public void onPress() {
        LocalPlayer player = Minecraft.getInstance().player;
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
