package com.fox.ysmu.client.gui.button;

import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.client.ClientModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class TextureCountButton extends FlatColorButton {
    public TextureCountButton(int id, int x, int y) {
        super(id, x, y, 20, 20, "");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        // 在绘制前更新显示文本
        this.updateDisplayString();
        // 调用父类方法进行绘制
        super.drawButton(mc, mouseX, mouseY);
    }

    private void updateDisplayString() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            ExtendedModelInfo eep = ExtendedModelInfo.get(player);
            if (eep != null) {
                ResourceLocation modelId = eep.getModelId();
                if (ClientModelManager.MODELS.containsKey(modelId)) {
                    this.displayString = String.valueOf(ClientModelManager.MODELS.get(modelId).size());
                    return;
                }
            }
        }
        this.displayString = "";
    }
}
