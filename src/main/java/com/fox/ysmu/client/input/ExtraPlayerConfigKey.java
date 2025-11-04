// package com.fox.ysmu.client.input;
//
// import com.fox.ysmu.ysmu;
// import com.fox.ysmu.client.gui.ExtraPlayerConfigScreen;
// import com.mojang.blaze3d.platform.InputConstants;
// import net.minecraft.client.KeyMapping;
// import net.minecraft.client.Minecraft;
// import net.minecraftforge.api.distmarker.Dist;
// import net.minecraftforge.client.event.InputEvent;
// import net.minecraftforge.client.settings.KeyConflictContext;
// import net.minecraftforge.client.settings.KeyModifier;
// import net.minecraftforge.eventbus.api.SubscribeEvent;
// import net.minecraftforge.fml.common.Mod;
// import org.lwjgl.glfw.GLFW;
//
// @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ysmu.MODID)
// public class ExtraPlayerConfigKey {
// public static final KeyMapping EXTRA_PLAYER_RENDER_KEY = new
// KeyMapping("key.yes_steve_model.open_extra_player_render.desc",
// KeyConflictContext.IN_GAME,
// KeyModifier.ALT,
// InputConstants.Type.KEYSYM,
// GLFW.GLFW_KEY_P,
// "key.category.yes_steve_model");
//
// @SubscribeEvent
// public static void onKeyboardInput(InputEvent.Key event) {
// if (EXTRA_PLAYER_RENDER_KEY.isDown()) {
// Minecraft.getInstance().setScreen(new ExtraPlayerConfigScreen());
// }
// }
// }
