package com.fox.ysmu.client.gui.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

public class ConfigCheckBox extends GuiButton {
    //private final ForgeConfigSpec.BooleanValue configSpec;
    //移除 ForgeConfigSpec，这个按钮只负责UI状态，配置的读写应由使用它的Screen负责
    private boolean isChecked;

    public ConfigCheckBox(int id, int pX, int pY, String key, boolean isChecked) {
        super(id, pX, pY, 400, 20, I18n.format("gui.yes_steve_model.config." + key));
        this.isChecked = isChecked;
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            this.isChecked = !this.isChecked;
            return true;
        }
        return false;
    }

    public boolean isChecked() {
        return this.isChecked;
    }
}
