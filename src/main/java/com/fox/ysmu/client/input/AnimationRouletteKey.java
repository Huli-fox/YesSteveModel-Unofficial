package com.fox.ysmu.client.input;

import com.fox.ysmu.client.gui.AnimationRouletteScreen;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

@EventBusSubscriber(side = Side.CLIENT)
public class AnimationRouletteKey {
    public static final KeyBinding ANIMATION_ROULETTE_KEY =
        new KeyBinding("key.yes_steve_model.animation_roulette.desc", Keyboard.KEY_Z, "key.category.yes_steve_model");

    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.KeyInputEvent event) {
        if (ANIMATION_ROULETTE_KEY.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new AnimationRouletteScreen());
        }
    }
}
