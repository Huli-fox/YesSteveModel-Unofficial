package com.fox.ysmu.client;

import com.fox.ysmu.CommonProxy;
import com.fox.ysmu.client.animation.AnimationRegister;
import com.fox.ysmu.client.renderer.CustomPlayerRenderer;

import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    private static CustomPlayerRenderer CUSTOM_PLAYER_RENDERER;

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        AnimationRegister.registerAnimationState();
        AnimationRegister.registerVariables();
        CUSTOM_PLAYER_RENDERER = new CustomPlayerRenderer();
    }

    public static CustomPlayerRenderer getInstance() {
        return CUSTOM_PLAYER_RENDERER;
    }
}
