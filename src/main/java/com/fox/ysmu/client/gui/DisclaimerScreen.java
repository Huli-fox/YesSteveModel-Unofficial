package com.fox.ysmu.client.gui;

import com.fox.ysmu.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class DisclaimerScreen extends Screen {
    private Checkbox readCheckbox;
    private int x;
    private int y;

    public DisclaimerScreen() {
        super(Component.literal("Disclaimer GUI"));
    }

    @Override

    protected void init() {
        this.clearWidgets();

        MutableComponent mainText = Component.translatable("gui.yes_steve_model.disclaimer.text");
        List<FormattedCharSequence> splitMainText = font.split(mainText, 400);
        int totalHeight = splitMainText.size() * font.lineHeight + 20 + 20 + 10 + 20;
        this.x = (width - 400) / 2;
        this.y = (height - totalHeight) / 2;

        MutableComponent readCheckboxText = Component.translatable("gui.yes_steve_model.disclaimer.read");
        int readTextWidth = font.width(readCheckboxText);
        readCheckbox = new Checkbox((width - readTextWidth) / 2, y + totalHeight - 50, readTextWidth, 20, readCheckboxText, !Config.DISCLAIMER_SHOW);
        addRenderableWidget(readCheckbox);
        addRenderableWidget(new Button.Builder(Component.translatable("gui.yes_steve_model.disclaimer.close"), b -> {
            if (readCheckbox.selected()) {
                Config.DISCLAIMER_SHOW = false;
                Minecraft.getInstance().setScreen(new PlayerModelScreen());
            } else {
                Minecraft.getInstance().setScreen(null);
            }
        }).size(300, 20).pos((width - 300) / 2, y + totalHeight - 20).build());
    }

    @Override

    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(graphics);
        graphics.drawWordWrap(font, Component.translatable("gui.yes_steve_model.disclaimer.text"), x, y, 400, 0xffffffff);
        super.render(graphics, pMouseX, pMouseY, pPartialTick);
    }
}
