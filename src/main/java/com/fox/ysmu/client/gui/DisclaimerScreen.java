package com.fox.ysmu.client.gui;

import com.fox.ysmu.Config;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.util.List;

public class DisclaimerScreen extends GuiScreen {
    private GuiButton readCheckbox;
    private boolean hasAgreed;
    private int x;
    private int y;

    @Override
    public void initGui() {
        hasAgreed = !Config.DISCLAIMER_SHOW;
        this.buttonList.clear();

        String mainText = I18n.format("gui.yes_steve_model.disclaimer.text");
        List<String> splitMainText = this.fontRendererObj.listFormattedStringToWidth(mainText, 400);
        int totalHeight = splitMainText.size() * this.fontRendererObj.FONT_HEIGHT + 20 + 20 + 10 + 20;
        x = (this.width - 400) / 2;
        y = (this.height - totalHeight) / 2;

        String readCheckboxText = (hasAgreed ? "[X] " : "[ ] ") + I18n.format("gui.yes_steve_model.disclaimer.read");
        int readTextWidth = this.fontRendererObj.getStringWidth(readCheckboxText) + 25; // 为 "[X] " 留出空间
        readCheckbox = new GuiButton(0, (this.width - readTextWidth) / 2, y + totalHeight - 50, readTextWidth, 20, readCheckboxText);
        this.buttonList.add(readCheckbox);
        this.buttonList.add(new GuiButton(1, (width - 300) / 2, y + totalHeight - 20, 300, 20, I18n.format("gui.yes_steve_model.disclaimer.close")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                hasAgreed = !hasAgreed;
                button.displayString = (hasAgreed ? "[X] " : "[ ] ") + I18n.format("gui.yes_steve_model.disclaimer.read");
                break;
            case 1:
                if (hasAgreed) {
                    Config.DISCLAIMER_SHOW = false;
                    this.mc.displayGuiScreen(new PlayerModelScreen());
                } else {
                    this.mc.displayGuiScreen(null);
                }
                break;
        }
    }
    @Override
    public void drawScreen(int pMouseX, int pMouseY, float pPartialTick) {
        this.drawDefaultBackground();
        this.fontRendererObj.drawSplitString(I18n.format("gui.yes_steve_model.disclaimer.text"), x, y, 400, 0xffffffff);
        super.drawScreen(pMouseX, pMouseY, pPartialTick);
    }
}
