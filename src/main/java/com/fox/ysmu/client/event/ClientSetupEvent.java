package com.fox.ysmu.client.event;

import com.fox.ysmu.ysmu;
import com.fox.ysmu.client.animation.AnimationRegister;
//import com.fox.ysmu.client.gui.DebugAnimationScreen;
//import com.fox.ysmu.client.gui.ExtraPlayerScreen;
import com.fox.ysmu.client.input.*;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
//import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.ModList;
//import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

//import static net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.DEBUG_TEXT;


public class ClientSetupEvent {
    @SubscribeEvent
    public static void onClientSetup(FMLInitializationEvent event) {
        AnimationRegister.registerAnimationState();
        AnimationRegister.registerVariables();
    }

//    @SubscribeEvent
//    public static void onClientSetup(RegisterKeyMappingsEvent event) {
//        event.register(PlayerModelScreenKey.PLAYER_MODEL_KEY);
//        event.register(AnimationRouletteKey.ANIMATION_ROULETTE_KEY);
//        event.register(DebugAnimationKey.DEBUG_ANIMATION_KEY);
//        event.register(ExtraPlayerConfigKey.EXTRA_PLAYER_RENDER_KEY);
//        ExtraAnimationKey.registerKeyBinding(event);
//    }

//    @SubscribeEvent
//    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
//        event.registerAbove(DEBUG_TEXT.id(), "ysm_debug_info", new DebugAnimationScreen());
//        event.registerAbove(DEBUG_TEXT.id(), "ysm_extra_player", new ExtraPlayerScreen());
//    }
}
