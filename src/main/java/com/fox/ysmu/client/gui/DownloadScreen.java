package com.fox.ysmu.client.gui;

import com.fox.ysmu.client.gui.button.FlatColorButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public class DownloadScreen extends GuiScreen {
    private final PlayerModelScreen parent;
    private int x;
    private int y;

    public DownloadScreen(PlayerModelScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.x = (width - 420) / 2;
        this.y = (height - 235) / 2;
        this.buttonList.add(new FlatColorButton(0, x + 5, y, 80, 18, I18n.format("gui.yes_steve_model.model.return")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            this.mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void drawScreen(int pMouseX, int pMouseY, float pPartialTick) {
        this.drawDefaultBackground();
        this.drawCenteredString(fontRendererObj, "Coming S∞n", width / 2, height / 2 - 5, 0xAA0000);
        super.drawScreen(pMouseX, pMouseY, pPartialTick);
    }
}
