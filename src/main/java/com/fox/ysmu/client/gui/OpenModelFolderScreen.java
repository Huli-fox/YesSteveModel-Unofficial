package com.fox.ysmu.client.gui;

import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.util.Keep;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class OpenModelFolderScreen extends Screen {
    private final PlayerModelScreen screen;

    protected OpenModelFolderScreen(PlayerModelScreen screen) {
        super(Component.literal("Open Model Folder"));
        this.screen = screen;
    }

    @Override
    @Keep
    protected void init() {
        int x = (width - 310) / 2;
        int y = height / 2 + 60;
        this.clearWidgets();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.yes_steve_model.open_model_folder.open"), b -> {
            Util.getPlatform().openFile(ServerModelManager.CUSTOM.toFile());
        }).bounds(x, y, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.yes_steve_model.model.return"), b -> {
            getMinecraft().setScreen(this.screen);
        }).bounds(x + 160, y, 150, 20).build());
    }

    @Override
    @Keep
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(graphics);
        graphics.drawWordWrap(font, Component.translatable("gui.yes_steve_model.open_model_folder.tips"),
                (width - 400) / 2, height / 2 - 80, 400, 0XFFFFFF);
        super.render(graphics, pMouseX, pMouseY, pPartialTick);
    }
}
