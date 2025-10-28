//package com.fox.ysmu.client.input;
//
//import com.fox.ysmu.ysmu;
//import com.mojang.blaze3d.platform.InputConstants;
//import net.minecraft.client.KeyMapping;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.player.LocalPlayer;
//import net.minecraft.network.chat.Component;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.client.event.InputEvent;
//import net.minecraftforge.client.settings.KeyConflictContext;
//import net.minecraftforge.client.settings.KeyModifier;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//import org.lwjgl.glfw.GLFW;
//
//@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ysmu.MODID)
//public class DebugAnimationKey {
//    public static boolean DEBUG = false;
//
//    public static final KeyMapping DEBUG_ANIMATION_KEY = new KeyMapping("key.yes_steve_model.debug_animation.desc",
//            KeyConflictContext.IN_GAME, KeyModifier.ALT,
//            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B,
//            "key.category.yes_steve_model");
//
//    @SubscribeEvent
//    public static void onKeyboardInput(InputEvent.Key event) {
//        if (DEBUG_ANIMATION_KEY.isDown()) {
//            DEBUG = !DEBUG;
//            LocalPlayer player = Minecraft.getInstance().player;
//            if (player == null) {
//                return;
//            }
//            if (DEBUG) {
//                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("message.yes_steve_model.model.debug_animation.true"));
//            } else {
//                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("message.yes_steve_model.model.debug_animation.false"));
//            }
//        }
//    }
//}
