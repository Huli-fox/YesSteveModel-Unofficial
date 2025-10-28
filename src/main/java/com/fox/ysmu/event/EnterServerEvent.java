package com.fox.ysmu.event;

import com.fox.ysmu.ysmu;
import com.fox.ysmu.model.ServerModelManager;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ysmu.MODID)
public final class EnterServerEvent {
    @SubscribeEvent
    public static void onLoggedInServer(PlayerEvent.PlayerLoggedInEvent event) {
        ServerModelManager.sendRequestSyncModelMessage(event.getEntity());
    }
}
