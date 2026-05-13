package com.gts.ysmu.event;

import com.gts.ysmu.model.ServerModelManager;
import com.gts.ysmu.YesSteveModel;
import com.gts.ysmu.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PlayerLogoutEvent {
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer serverPlayer) {
            if (NetworkHandler.isPlayerConnected(serverPlayer)) {
                ServerModelManager.syncModelToPlayer(serverPlayer.getUUID());
            }
        }
    }
}
