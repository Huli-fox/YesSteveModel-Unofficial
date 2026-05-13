package com.gts.ysmu.event;

import com.gts.ysmu.YesSteveModel;
import com.gts.ysmu.network.NetworkHandler;
import com.gts.ysmu.network.message.S2CVersionCheckPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class EnterServerEvent {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        Player entity = event.getEntity();
        if (entity instanceof ServerPlayer) {
            NetworkHandler.sendToClientPlayer(new S2CVersionCheckPacket(), entity);
        }
    }
}
