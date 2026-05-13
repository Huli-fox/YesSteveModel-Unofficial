package com.gts.ysmu.client.event;

import com.gts.ysmu.client.ClientModelManager;
import com.gts.ysmu.YesSteveModel;
import com.gts.ysmu.capability.PlayerCapabilityProvider;
import com.gts.ysmu.client.upload.UploadManager;
import com.gts.ysmu.audio.ObjectPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class ClientTickEvent {

    private static int tickCount;

    private static int refreshRate = 60;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!YesSteveModel.isAvailable() || event.phase == TickEvent.Phase.END) {
            return;
        }
        tickCount++;
        UploadManager.processPendingUploads();
        ClientModelManager.flushPendingModels();
        ObjectPool.cleanup();
        refreshRate = Minecraft.getInstance().getWindow().getRefreshRate();
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            localPlayer.getCapability(PlayerCapabilityProvider.PLAYER_CAP).ifPresent((v0) -> {
                v0.tickAnimations();
            });
        }
    }

    public static int getTickCount() {
        return tickCount;
    }

    public static int getRefreshRate() {
        return refreshRate;
    }
}
