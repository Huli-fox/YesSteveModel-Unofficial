package com.fox.ysmu.client.gui;

import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.client.input.ExtraAnimationKey;
import com.fox.ysmu.Config;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.SetPlayAnimation;
import com.fox.ysmu.util.Keep;
import com.fox.ysmu.util.ModelIdUtil;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix4f;

public class AnimationRouletteScreen extends Screen {
    private int x;
    private int y;
    private int selectId = -1;
    private String[] names;

    public AnimationRouletteScreen() {
        super(Component.literal("Animation Roulette GUI"));
    }

    @Override
    @Keep
    protected void init() {
        this.x = width / 2;
        this.y = height / 2 - 8;

        if (minecraft != null && minecraft.player != null) {
            ExtendedModelInfo eep = ExtendedModelInfo.get(minecraft.player);
            if (eep != null) {
                ResourceLocation modelId = eep.getModelId();
                if (ClientModelManager.EXTRA_ANIMATION_NAME.containsKey(ModelIdUtil.getMainId(modelId))) {
                    this.names = ClientModelManager.EXTRA_ANIMATION_NAME.get(ModelIdUtil.getMainId(modelId));
                }
            }
        }
    }

    @Override
    @Keep
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        drawRoulette(graphics.pose(), pMouseX, pMouseY);
        drawRouletteText(graphics);
    }

    @Override
    @Keep
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (-1 < selectId && selectId < 8 && minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            NetworkHandler.CHANNEL.sendToServer(new SetPlayAnimation(selectId));
            if (minecraft.player != null && Config.PRINT_ANIMATION_ROULETTE_MSG) {
                minecraft.player.sendSystemMessage(Component.translatable("message.yes_steve_model.model.animation_roulette.play", selectId));
            }
            minecraft.setScreen(null);
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    @Keep
    public boolean isPauseScreen() {
        return false;
    }

    private void drawRouletteText(GuiGraphics graphics) {
        int count = 8;
        float startDeg = Mth.PI / count;
        for (int i = 0; i < count; i++) {
            int r = 65;
            MutableComponent keyText = Component.literal("[ ").withStyle(ChatFormatting.YELLOW);
            KeyMapping keyMapping = ExtraAnimationKey.EXTRA_ANIMATION_KEYS.get(i);
            if (keyMapping.getKey() == InputConstants.UNKNOWN) {
                keyText.append(Component.translatable("key.yes_steve_model.extra_animation.none"));
            } else {
                keyText.append(keyMapping.getTranslatedKeyMessage());
            }
            keyText.append(" ]");
            if (this.names != null && this.names.length > i && StringUtils.isNoneBlank(this.names[i])) {
                graphics.drawCenteredString(font, Component.literal(this.names[i]), (int) (x + r * Mth.cos(startDeg)), (int) (y + r * Mth.sin(startDeg) - font.lineHeight / 2 - 8), 0xF3EFE0);
            } else {
                graphics.drawCenteredString(font, String.valueOf(i), (int) (x + r * Mth.cos(startDeg)), (int) (y + r * Mth.sin(startDeg) - font.lineHeight / 2 - 8), 0xF3EFE0);
            }
            graphics.drawCenteredString(font, keyText, (int) (x + r * Mth.cos(startDeg)), (int) (y + r * Mth.sin(startDeg) - font.lineHeight / 2 + 4), 0xF3EFE0);
            startDeg = startDeg + 2 * Mth.PI / count;
        }
    }

    private void drawRoulette(PoseStack pPoseStack, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f pMatrix = pPoseStack.last().pose();

        int count = 8;
        float theta = (float) Mth.atan2(mouseY - y, mouseX - x);
        if (theta < 0) {
            theta = Mth.PI * 2 + theta;
        }
        float distance = Mth.sqrt(Mth.square(mouseY - y) + Mth.square(mouseX - x));
        boolean isSelected = false;
        for (int i = 0; i < count; i++) {
            float spacingDeg = Mth.PI / 90;
            float startDeg = (2 * Mth.PI / count) * i + spacingDeg;
            float endDeg = (2 * Mth.PI / count) * (i + 1) - spacingDeg;
            if (startDeg < theta && theta < endDeg && 50 < distance && distance < 100) {
                drawFan(bufferbuilder, pMatrix, 25, 105, startDeg, endDeg, 0xf0FFB100);
                isSelected = true;
                this.selectId = i;
            } else {
                drawFan(bufferbuilder, pMatrix, 25, 105, startDeg, endDeg, 0x90000000);
            }
        }
        if (!isSelected) {
            this.selectId = -1;
        }

        tesselator.end();
        RenderSystem.disableBlend();
    }

    private void drawFan(BufferBuilder builder, Matrix4f matrix4f, float rIn, float rOut, float startDeg, float endDeg, int color) {
        float alpha = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        builder.vertex(matrix4f, x + rOut * Mth.cos(startDeg), y + rOut * Mth.sin(startDeg), 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix4f, x + rIn * Mth.cos(startDeg), y + rIn * Mth.sin(startDeg), 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix4f, x + rIn * Mth.cos(endDeg), y + rIn * Mth.sin(endDeg), 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix4f, x + rOut * Mth.cos(endDeg), y + rOut * Mth.sin(endDeg), 0).color(red, green, blue, alpha).endVertex();
    }
}
