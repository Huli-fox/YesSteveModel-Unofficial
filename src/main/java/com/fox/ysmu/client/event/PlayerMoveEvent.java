package com.fox.ysmu.client.event;

import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.SetPlayAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

public class PlayerMoveEvent {
    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.KeyInputEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (isMoveKey() && player != null) {
            ExtendedModelInfo eep = ExtendedModelInfo.get(player);
                if (eep != null && eep.isPlayAnimation()) {
                    NetworkHandler.CHANNEL.sendToServer(SetPlayAnimation.stop());
                }
        }
    }

    private static boolean isMoveKey() {
        KeyBinding[] keyBindings = Minecraft.getMinecraft().gameSettings.keyBindings;
        for (KeyBinding keyBinding : keyBindings) {
            if ((keyBinding == Minecraft.getMinecraft().gameSettings.keyBindForward ||
                 keyBinding == Minecraft.getMinecraft().gameSettings.keyBindBack ||
                 keyBinding == Minecraft.getMinecraft().gameSettings.keyBindLeft ||
                 keyBinding == Minecraft.getMinecraft().gameSettings.keyBindRight ||
                 keyBinding == Minecraft.getMinecraft().gameSettings.keyBindJump ||
                 keyBinding == Minecraft.getMinecraft().gameSettings.keyBindSneak) && keyBinding.isPressed()) {
                return true;
            }
        }
        return false;
    }
}