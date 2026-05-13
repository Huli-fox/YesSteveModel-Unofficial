package com.gts.ysmu.client.event;

import com.gts.ysmu.YesSteveModel;
import com.gts.ysmu.capability.PlayerCapabilityProvider;
import com.gts.ysmu.network.NetworkHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class ClientPlayerCloneEvent {
    @SubscribeEvent
    public static void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        if (!YesSteveModel.isAvailable() || !NetworkHandler.isClientConnected()) {
            return;
        }
        event.getOldPlayer().reviveCaps();
        event.getOldPlayer().getCapability(PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap -> event.getNewPlayer().getCapability(PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap2 -> cap2.copyFrom(cap)));
        event.getOldPlayer().invalidateCaps();
    }
}
