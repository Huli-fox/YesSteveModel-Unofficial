package com.fox.ysmu.client.input;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Keyboard;

@EventBusSubscriber(side = Side.CLIENT)
public class DebugAnimationKey {
    public static boolean DEBUG = false;

    public static final KeyBinding DEBUG_ANIMATION_KEY =
        new KeyBinding("key.yes_steve_model.debug_animation.desc", Keyboard.KEY_B, "key.category.yes_steve_model");

    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.KeyInputEvent event) {
        boolean isAltKeyDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        if (DEBUG_ANIMATION_KEY.isPressed() && isAltKeyDown) {
            DEBUG = !DEBUG;
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (player == null) {
                return;
            }
            if (DEBUG) {
                player.addChatMessage(new ChatComponentText(I18n.format("message.yes_steve_model.model.debug_animation.true")));
            } else {
                player.addChatMessage(new ChatComponentText(I18n.format("message.yes_steve_model.model.debug_animation.false")));
            }
        }
    }
}
