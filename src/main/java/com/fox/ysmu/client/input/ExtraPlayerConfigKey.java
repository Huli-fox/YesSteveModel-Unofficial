package com.fox.ysmu.client.input;

import com.fox.ysmu.client.gui.ExtraPlayerConfigScreen;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

@EventBusSubscriber(side = Side.CLIENT)
public class ExtraPlayerConfigKey {
    public static final KeyBinding EXTRA_PLAYER_RENDER_KEY =
        new KeyBinding("key.yes_steve_model.open_extra_player_render.desc", Keyboard.KEY_P, "key.category.yes_steve_model");

    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.KeyInputEvent event) {
        boolean isAltKeyDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        if (EXTRA_PLAYER_RENDER_KEY.isPressed() && isAltKeyDown) {
            Minecraft.getMinecraft().displayGuiScreen(new ExtraPlayerConfigScreen());
        }
    }
}
