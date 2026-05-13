package com.gts.ysmu.event;

import com.gts.ysmu.capability.*;
import com.gts.ysmu.client.ClientModelManager;
import com.gts.ysmu.model.ServerModelManager;
import com.gts.ysmu.network.NetworkHandler;
import com.gts.ysmu.capability.VehicleCapability;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.io.IOException;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CommonEvent {
    public static Object initModelManagers() {
        ClientModelManager.loadDefaultModel();
        try {
            ServerModelManager.reloadPacks();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.init();
            initModelManagers();
        });
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ModelInfoCapability.class);
        event.register(ProjectileModelCapability.class);
        event.register(VehicleModelCapability.class);
        event.register(StarModelsCapability.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            event.register(PlayerCapability.class);
            event.register(ProjectileCapability.class);
            event.register(VehicleCapability.class);
        }
    }
}
