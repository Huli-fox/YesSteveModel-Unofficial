package com.fox.ysmu.client;

import com.fox.ysmu.client.entity.CustomPlayerEntity;
import net.minecraft.client.Minecraft;

import com.fox.ysmu.CommonProxy;
import com.fox.ysmu.client.animation.AnimationRegister;
import com.fox.ysmu.client.renderer.CustomPlayerRenderer;
import com.fox.ysmu.eep.ExtendedAuthModels;
import com.fox.ysmu.eep.ExtendedStarModels;
import com.fox.ysmu.network.message.SyncAuthModels;
import com.fox.ysmu.network.message.SyncStarModels;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import software.bernie.geckolib3.geo.GeoReplacedEntityRenderer;

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
        GeoReplacedEntityRenderer.registerReplacedEntity(CustomPlayerEntity.class, CUSTOM_PLAYER_RENDERER);
    }

    public static CustomPlayerRenderer getInstance() {
        return CUSTOM_PLAYER_RENDERER;
    }

    @Override
    public void handleAuthModels(SyncAuthModels message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            ExtendedAuthModels eep = ExtendedAuthModels.get(mc.thePlayer);
            if (eep != null) {
                eep.setAuthModels(message.getAuthModels());
            }
        }
    }

    @Override
    public void handleStarModels(SyncStarModels message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            ExtendedStarModels eep = ExtendedStarModels.get(mc.thePlayer);
            if (eep != null) {
                eep.setStarModels(message.getStarModels());
            }
        }
    }
}
