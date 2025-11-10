package com.fox.ysmu.client.gui.button;

import com.fox.ysmu.ysmu;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.eep.ExtendedStarModels;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.OpenModelGuiMessage;
import com.fox.ysmu.network.message.SetModelAndTexture;
import com.fox.ysmu.network.message.SetNpcModelAndTexture;
import com.fox.ysmu.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class ModelButton extends GuiButton {
    private final static ResourceLocation ICON = new ResourceLocation(ysmu.MODID, "texture/icon.png");
    private final Pair<ResourceLocation, List<ResourceLocation>> modelInfo;
    private final boolean needAuth;
    private final int color;
    private final List<String> tooltips;
    private final EntityPlayer player;

    public ModelButton(int id, int pX, int pY, boolean needAuth, Pair<ResourceLocation, List<ResourceLocation>> modelInfo,
                       List<String> tooltips, EntityPlayer player) {
        super(id, pX, pY, 52, 90, ""); // TODO 如果下面不手动设置，“”为下面的值
        this.modelInfo = modelInfo;
        this.needAuth = needAuth;
        this.color = needAuth ? 0x7F_000000 : 0xFF_434242;
        this.tooltips = tooltips;
        this.player = player;
        // displayString 用于显示在按钮上的文字，这里我们手动渲染更复杂的文字
        this.displayString = modelInfo.getLeft().getResourcePath();
    }

    // TODO 没改完
    public void doPress() {
        if (this.needAuth) {
            return;
        }
        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep != null) {
            eep.setModelAndTexture(modelInfo.getLeft(), modelInfo.getRight().get(0));
        }
        if (player.equals(Minecraft.getMinecraft().thePlayer)) {
            NetworkHandler.CHANNEL.sendToServer(new SetModelAndTexture(modelInfo.getLeft(), modelInfo.getRight().get(0)));
        } else {
            NetworkHandler.CHANNEL.sendToServer(new SetNpcModelAndTexture(modelInfo.getLeft(), modelInfo.getRight().get(0), OpenModelGuiMessage.CURRENT_NPC_ID));
        }
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        FontRenderer font = mc.fontRenderer;
        // Hover状态
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
        // 绘制背景(原graphics.fillGradient)
        this.drawGradientRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, this.color, this.color);
        // 剪裁测试（缩放）
        int scale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        int scissorX = this.xPosition * scale;
        // 在GL11中，Y轴的原点在左下角，所以需要从屏幕总高度中减去
        int scissorY = mc.displayHeight - ((this.yPosition + this.height - 20) * scale);
        int scissorW = this.width * scale;
        int scissorH = (this.height - 20) * scale;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
        // 渲染实体
        RenderUtil.renderEntityInInventory(this.xPosition + this.width / 2, this.yPosition + this.height / 2 + 20, 30,
            mc.thePlayer, modelInfo.getLeft(), modelInfo.getRight().get(0));
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        // 渲染文本
        // font.split->listFormattedStringToWidth
        List<String> split = font.listFormattedStringToWidth(this.displayString, 45);
        if (split.size() > 1) {
            this.drawCenteredString(font, split.get(0), this.xPosition + this.width / 2, this.yPosition + this.height - 19, 0xF3EFE0);
            this.drawCenteredString(font, split.get(1), this.xPosition + this.width / 2, this.yPosition + this.height - 10, 0xF3EFE0);
        } else {
            this.drawCenteredString(font, this.displayString, this.xPosition + this.width / 2, this.yPosition + this.height - 15, 0xF3EFE0);
        }
        // 悬停时的高亮边框
        if (!this.needAuth && this.field_146123_n) {
            this.drawGradientRect(this.xPosition, this.yPosition + 1, this.xPosition + 1, this.yPosition + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            this.drawGradientRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + 1, 0xff_F3EFE0, 0xff_F3EFE0);
            this.drawGradientRect(this.xPosition + this.width - 1, this.yPosition + 1, this.xPosition + this.width, this.yPosition + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            this.drawGradientRect(this.xPosition, this.yPosition + this.height - 1, this.xPosition + this.width, this.yPosition + this.height, 0xff_F3EFE0, 0xff_F3EFE0);
        }
        // 收藏图标
        ExtendedStarModels eep = ExtendedStarModels.get(player);
        if (eep != null && eep.containModel(modelInfo.getLeft())) {
            // graphics.blit
            mc.getTextureManager().bindTexture(ICON);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(this.xPosition + this.width - 14, this.yPosition, 16, 0, 16, 16);
        }
        if (needAuth) {
            this.drawGradientRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, 0x9F222222, 0x9F222222);
        }
    }

    // Tooltip 的渲染需要从外部的 GuiScreen 调用
    public void renderTooltip(GuiScreen screen, int pMouseX, int pMouseY) {
        if (this.field_146123_n && tooltips != null && !tooltips.isEmpty()) {
            // func_146283_a:drawHoveringText
            screen.func_146283_a(tooltips, pMouseX, pMouseY);
        }
    }
}
