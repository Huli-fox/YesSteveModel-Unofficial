package com.gts.ysmu.client.event;

import com.gts.ysmu.YesSteveModel;
import com.gts.ysmu.capability.PlayerCapabilityProvider;
import com.gts.ysmu.client.renderer.RendererManager;
import com.gts.ysmu.config.GeneralConfig;
import com.gts.ysmu.util.CameraUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class ReplacePlayerRenderEvent {
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        Player entity = event.getEntity();
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (entity.equals(localPlayer) && GeneralConfig.DISABLE_SELF_MODEL.get().booleanValue()) {
            return;
        }
        if ((!entity.equals(localPlayer) && GeneralConfig.DISABLE_OTHER_MODEL.get().booleanValue()) || event.getEntity().isSpectator()) {
            return;
        }
        entity.getCapability(PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap -> {
            if (cap.isModelActive()) {
                if (!CameraUtil.isFirstPerson(cap)) {
                    event.setCanceled(true);
                    RendererManager.getPlayerRenderer().render(event.getEntity(), event.getEntity().getYRot(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
                }
            }
        });
    }
}
