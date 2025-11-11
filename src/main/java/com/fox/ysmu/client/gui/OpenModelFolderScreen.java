package com.fox.ysmu.client.gui;

import com.fox.ysmu.model.ServerModelManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.awt.Desktop;
import java.io.File;

public class OpenModelFolderScreen extends GuiScreen {
    private final PlayerModelScreen screen;

    protected OpenModelFolderScreen(PlayerModelScreen screen) {
        this.screen = screen;
    }

    @Override
    public void initGui() {
        int x = (this.width - 310) / 2;
        int y = this.height / 2 + 60;
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, x, y, 150, 20, I18n.format("gui.yes_steve_model.open_model_folder.open")));
        this.buttonList.add(new GuiButton(1, x + 160, y, 150, 20, I18n.format("gui.yes_steve_model.model.return")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                try {
                    File modelFolder = ServerModelManager.CUSTOM.toFile();
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(modelFolder);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 1:
                this.mc.displayGuiScreen(this.screen);
                break;
        }
    }

    @Override
    public void drawScreen(int pMouseX, int pMouseY, float pPartialTick) {
        this.drawDefaultBackground();
        // 绘制自动换行的文本 drawWordWrap -> drawSplitString
        this.fontRendererObj.drawSplitString(I18n.format("gui.yes_steve_model.open_model_folder.tips"),
                (this.width - 400) / 2, this.height / 2 - 80, 400, 0XFFFFFF);
        super.drawScreen(pMouseX, pMouseY, pPartialTick);
    }
}
