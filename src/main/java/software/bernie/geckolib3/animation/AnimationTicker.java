package software.bernie.geckolib3.animation;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import software.bernie.geckolib3.core.manager.AnimationData;

public class AnimationTicker {

    private final AnimationData data;

    public AnimationTicker(AnimationData data) {
        this.data = data;
    }

    @SubscribeEvent
    public void tickEvent(TickEvent.ClientTickEvent event) {
        if (Minecraft.getMinecraft()
            .isGamePaused() && !data.shouldPlayWhilePaused) {
            return;
        }

        if (event.phase == TickEvent.Phase.END) {
            data.tick++;
        }
    }
}
