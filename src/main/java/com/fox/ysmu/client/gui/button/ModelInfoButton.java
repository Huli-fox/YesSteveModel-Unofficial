package com.fox.ysmu.client.gui.button;

import com.fox.ysmu.model.format.Type;
import com.fox.ysmu.network.message.RequestServerModelInfo;
import com.fox.ysmu.util.FileSizeUtils;
import com.fox.ysmu.util.Keep;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ModelInfoButton extends Button {
    private final RequestServerModelInfo.Info info;
    private boolean isSelect = false;

    public ModelInfoButton(int pX, int pY, int pHeight, RequestServerModelInfo.Info info, OnPress pOnPress) {
        super(pX, pY, 250, pHeight, Component.empty(), pOnPress, DEFAULT_NARRATION);
        this.info = info;
    }

    @Override
    @Keep
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float pPartialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        if (isSelect) {
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xff_1E90FF, 0xff_1E90FF);
        } else {
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xff_434242, 0xff_434242);
        }
        if (this.isHoveredOrFocused()) {
            graphics.fillGradient(this.getX(), this.getY() + 1, this.getX() + 1, this.getY() + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX() + this.width - 1, this.getY() + 1, this.getX() + this.width, this.getY() + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, 0xff_F3EFE0, 0xff_F3EFE0);
        }
        graphics.drawString(font, info.getFileName(), this.getX() + 5, this.getY() + (this.height - 8) / 2, 0xF3EFE0);
        if (info.getType() == Type.FOLDER) {
            graphics.drawString(font, Component.translatable("gui.yes_steve_model.model_manage.type.folder"), this.getX() + 155, this.getY() + (this.height - 8) / 2, ChatFormatting.AQUA.getColor());
        } else if (info.getType() == Type.ZIP) {
            graphics.drawString(font, Component.translatable("gui.yes_steve_model.model_manage.type.zip"), this.getX() + 155, this.getY() + (this.height - 8) / 2, ChatFormatting.GOLD.getColor());
        } else {
            graphics.drawString(font, Component.translatable("gui.yes_steve_model.model_manage.type.ysm"), this.getX() + 155, this.getY() + (this.height - 8) / 2, ChatFormatting.YELLOW.getColor());
        }
        graphics.drawString(font, FileSizeUtils.size(info.getSize()), this.getX() + 205, this.getY() + (this.height - 8) / 2, 0xC0C0C0);
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }
}
