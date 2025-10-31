package com.fox.ysmu.client.gui.button;

import com.fox.ysmu.ysmu;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.eep.ExtendedStarModels;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.bukkit.message.OpenModelGuiMessage;
import com.fox.ysmu.network.message.SetModelAndTexture;
import com.fox.ysmu.bukkit.message.SetNpcModelAndTexture;
import com.fox.ysmu.util.RenderUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class ModelButton extends Button {
    private final static ResourceLocation ICON = new ResourceLocation(ysmu.MODID, "texture/icon.png");
    private final Pair<ResourceLocation, List<ResourceLocation>> modelInfo;
    private final boolean needAuth;
    private final int color;
    private final List<Component> tooltips;
    private final Player player;

    public ModelButton(int pX, int pY, boolean needAuth, Pair<ResourceLocation, List<ResourceLocation>> modelInfo, List<Component> tooltips, Player player) {
        super(pX, pY, 52, 90, Component.literal(modelInfo.getLeft().getPath()), (b) -> {
        }, DEFAULT_NARRATION);
        this.modelInfo = modelInfo;
        this.needAuth = needAuth;
        this.color = needAuth ? 0x7F_000000 : 0xFF_434242;
        this.tooltips = tooltips;
        this.player = player;
    }

    @Override

    public void onPress() {
        if (needAuth) {
            return;
        }
        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep != null) {
            eep.setModelAndTexture(modelInfo.getLeft(), modelInfo.getRight().get(0));
        }
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (player.equals(localPlayer)) {
            NetworkHandler.CHANNEL.sendToServer(new SetModelAndTexture(modelInfo.getLeft(), modelInfo.getRight().get(0)));
        } else {
            NetworkHandler.CHANNEL.sendToServer(new SetNpcModelAndTexture(modelInfo.getLeft(), modelInfo.getRight().get(0), OpenModelGuiMessage.CURRENT_NPC_ID));
        }
    }

    @Override

    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, this.color, this.color);
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int scissorX = (int) (this.getX() * scale);
        int scissorY = (int) (window.getHeight() - ((this.getY() + this.height - 20) * scale));
        int scissorW = (int) (this.width * scale);
        int scissorH = (int) ((this.height - 20) * scale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        RenderUtil.renderEntityInInventory(this.getX() + this.width / 2, this.getY() + this.height / 2 + 20, 30, player, modelInfo.getLeft(), modelInfo.getRight().get(0));
        RenderSystem.disableScissor();

        Component message = this.getMessage();
        List<FormattedCharSequence> split = font.split(message, 45);
        if (split.size() > 1) {
            graphics.drawCenteredString(font, split.get(0), this.getX() + this.width / 2, this.getY() + this.height - 19, 0xF3EFE0);
            graphics.drawCenteredString(font, split.get(1), this.getX() + this.width / 2, this.getY() + this.height - 10, 0xF3EFE0);
        } else {
            graphics.drawCenteredString(font, this.getMessage(), this.getX() + this.width / 2, this.getY() + this.height - 15, 0xF3EFE0);
        }
        if (!this.needAuth && this.isHoveredOrFocused()) {
            graphics.fillGradient(this.getX(), this.getY() + 1, this.getX() + 1, this.getY() + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX() + this.width - 1, this.getY() + 1, this.getX() + this.width, this.getY() + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, 0xff_F3EFE0, 0xff_F3EFE0);
        }


        ExtendedStarModels eep = ExtendedStarModels.get(player);
        if (eep != null) {
            if (eep.containModel(modelInfo.getLeft())) {
                graphics.blit(ICON, this.getX() + this.width - 14, this.getY(), 16, 16, 16, 0, 16, 16, 256, 256);
            }
        }

        if (needAuth) {
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x9f_222222, 0x9f_222222);
        }
    }

    public void renderComponentTooltip(GuiGraphics graphics, Screen screen, int pMouseX, int pMouseY) {
        if (this.isHovered() && tooltips != null) {
            graphics.renderComponentTooltip(screen.getMinecraft().font, tooltips, pMouseX, pMouseY);
        }
    }


    @Override

    protected boolean clicked(double pMouseX, double pMouseY) {
        return !this.needAuth && super.clicked(pMouseX, pMouseY);
    }
}
