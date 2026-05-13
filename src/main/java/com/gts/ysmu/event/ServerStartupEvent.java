package com.gts.ysmu.event;

import com.gts.ysmu.model.ServerModelManager;
import com.gts.ysmu.YesSteveModel;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ServerStartupEvent {
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        ServerModelManager.loadModels(result -> {
            if (!result.isSuccess()) {
                event.getServer().execute(() -> {
                    throw new RuntimeException("YSM Loading Failed: " + result.getErrorMessage().getString(256));
                });
            }
        }, null);
    }
}
