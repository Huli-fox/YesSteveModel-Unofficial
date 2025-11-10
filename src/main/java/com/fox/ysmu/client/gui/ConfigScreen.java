package com.fox.ysmu.client.gui;

import com.fox.ysmu.client.gui.button.ConfigCheckBox;
import com.fox.ysmu.client.gui.button.FlatColorButton;
import com.fox.ysmu.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final PlayerModelScreen parent;

    public ConfigScreen(PlayerModelScreen parent) {
        super(Component.literal("YSM Config GUI"));
        this.parent = parent;
    }

    @Override

    public void initGui() {
        int x = (width - 420) / 2;
        int y = (height - 235) / 2;

        addRenderableWidget(new FlatColorButton(x + 5, y, 80, 18, Component.translatable("gui.yes_steve_model.model.return"), (b) -> this.getMinecraft().setScreen(parent)));

        addRenderableWidget(new ConfigCheckBox(x + 5, y + 25, "disable_self_model", Config.DISABLE_SELF_MODEL));
        addRenderableWidget(new ConfigCheckBox(x + 5, y + 47, "disable_other_model", Config.DISABLE_OTHER_MODEL));
        addRenderableWidget(new ConfigCheckBox(x + 5, y + 69, "print_animation_roulette_msg", Config.PRINT_ANIMATION_ROULETTE_MSG));
        addRenderableWidget(new ConfigCheckBox(x + 5, y + 91, "disable_self_hands", Config.DISABLE_SELF_HANDS));
        addRenderableWidget(new ConfigCheckBox(x + 5, y + 112, "disable_player_render", Config.DISABLE_PLAYER_RENDER));
    }

    @Override

    public void drawScreen(int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(graphics);
        super.render(graphics, pMouseX, pMouseY, pPartialTick);
    }
}
