package com.fox.ysmu.client.gui.button;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

public class ConfigCheckBox extends GuiButton {
    //移除 ForgeConfigSpec，这个按钮只负责UI状态，配置的读写应由使用它的Screen负责
    private boolean isChecked;
    private final String Key;

    public ConfigCheckBox(int id, int pX, int pY, String key, boolean isChecked) {
        super(id, pX, pY, 130, 20, "");
        this.Key = key;
        this.isChecked = isChecked;
        this.displayString = (isChecked ? "[X] " : "[ ] ") + I18n.format("gui.yes_steve_model.config." + key);
    }

    public void doPress() {
        this.isChecked = !this.isChecked;
        String translatedText = I18n.format("gui.yes_steve_model.config." + this.Key);
        this.displayString = (this.isChecked ? "[X] " : "[ ] ") + translatedText;
    }
}
