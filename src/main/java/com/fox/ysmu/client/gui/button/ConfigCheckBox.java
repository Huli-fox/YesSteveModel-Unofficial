package com.fox.ysmu.client.gui.button;

import com.fox.ysmu.util.Keep;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigCheckBox extends Checkbox {
    private final ForgeConfigSpec.BooleanValue configSpec;

    public ConfigCheckBox(int pX, int pY, String key, ForgeConfigSpec.BooleanValue configSpec) {
        super(pX, pY, 400, 20, Component.translatable("gui.yes_steve_model.config." + key), configSpec.get());
        this.configSpec = configSpec;
    }

    @Override
    @Keep
    public void onPress() {
        super.onPress();
        this.configSpec.set(!this.configSpec.get());
    }
}
