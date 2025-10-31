package com.fox.ysmu.client.gui.button;

import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.client.ClientModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;

public class TextureCountButton extends FlatColorButton {
    public TextureCountButton(int x, int y) {
        super(x, y, 20, 20, Component.empty(), (b) -> {
        });
    }

    @Override

    public Component getMessage() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ExtendedModelInfo eep = ExtendedModelInfo.get(player);
            if (eep != null) {
                ResourceLocation modelId = eep.getModelId();
                if (ClientModelManager.MODELS.containsKey(modelId)) {
                    String countText = String.valueOf(ClientModelManager.MODELS.get(modelId).size());
                    return Component.literal(countText);
                }
            }
            return super.getMessage();
        }
        return super.getMessage();
    }
}
