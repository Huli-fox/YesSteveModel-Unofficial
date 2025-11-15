package com.fox.ysmu.client.input;

import com.fox.ysmu.Config;
import com.fox.ysmu.client.gui.DisclaimerScreen;
import com.fox.ysmu.client.gui.PlayerModelScreen;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

@EventBusSubscriber(side = Side.CLIENT)
public class PlayerModelScreenKey {
    public static final KeyBinding PLAYER_MODEL_KEY =
        new KeyBinding("key.yes_steve_model.player_model.desc", Keyboard.KEY_Y, "key.category.yes_steve_model");

    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.KeyInputEvent event) {
        boolean isAltKeyDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        if (PLAYER_MODEL_KEY.isPressed() && isAltKeyDown) {
            if (Config.DISCLAIMER_SHOW) {
                Minecraft.getMinecraft().displayGuiScreen(new DisclaimerScreen());
            } else {
                Minecraft.getMinecraft().displayGuiScreen(new PlayerModelScreen());
            }
        }
    }
}
