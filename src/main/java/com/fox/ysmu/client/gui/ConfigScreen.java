package com.fox.ysmu.client.gui;

import com.fox.ysmu.client.gui.button.ConfigCheckBox;
import com.fox.ysmu.client.gui.button.FlatColorButton;
import com.fox.ysmu.Config;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public class ConfigScreen extends GuiScreen {
    private final PlayerModelScreen parent;

    public ConfigScreen(PlayerModelScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int x = (width - 420) / 2;
        int y = (height - 235) / 2;

        this.buttonList.add(new FlatColorButton(0, x + 5, y, 80, 18, I18n.format("gui.yes_steve_model.model.return")));
        this.buttonList.add(new ConfigCheckBox(1, x + 5, y + 25, "disable_self_model", Config.DISABLE_SELF_MODEL));
        this.buttonList.add(new ConfigCheckBox(2, x + 5, y + 47, "disable_other_model", Config.DISABLE_OTHER_MODEL));
        this.buttonList.add(new ConfigCheckBox(3, x + 5, y + 69, "print_animation_roulette_msg", Config.PRINT_ANIMATION_ROULETTE_MSG));
        this.buttonList.add(new ConfigCheckBox(4 ,x + 5, y + 91, "disable_self_hands", Config.DISABLE_SELF_HANDS));
        this.buttonList.add(new ConfigCheckBox(5, x + 5, y + 112, "disable_player_render", Config.DISABLE_PLAYER_RENDER));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                this.mc.displayGuiScreen(parent);
                break;
            case 1:
                Config.DISABLE_SELF_MODEL = !Config.DISABLE_SELF_MODEL;
                ((ConfigCheckBox) button).doPress();
                break;
            case 2:
                Config.DISABLE_OTHER_MODEL = !Config.DISABLE_OTHER_MODEL;
                ((ConfigCheckBox) button).doPress();
                break;
            case 3:
                Config.PRINT_ANIMATION_ROULETTE_MSG = !Config.PRINT_ANIMATION_ROULETTE_MSG;
                ((ConfigCheckBox) button).doPress();
                break;
            case 4:
                Config.DISABLE_SELF_HANDS = !Config.DISABLE_SELF_HANDS;
                ((ConfigCheckBox) button).doPress();
                break;
            case 5:
                Config.DISABLE_PLAYER_RENDER = !Config.DISABLE_PLAYER_RENDER;
                ((ConfigCheckBox) button).doPress();
                break;
        }
    }

    @Override
    public void drawScreen(int pMouseX, int pMouseY, float pPartialTick) {
        this.drawDefaultBackground();
        super.drawScreen(pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void onGuiClosed() {
        Config.save();
    }
}
