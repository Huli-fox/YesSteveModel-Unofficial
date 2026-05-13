package com.gts.ysmu.client.event;

import com.gts.ysmu.YesSteveModel;
import com.gts.ysmu.capability.PlayerCapabilityProvider;
import com.gts.ysmu.client.input.AnimationRouletteKey;
import com.gts.ysmu.network.NetworkHandler;
import com.gts.ysmu.network.message.C2SPlayAnimationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class AnimationLockEvent {

    private static boolean animationLocked = false;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (YesSteveModel.isAvailable() && event.getAction() == 1 && AnimationRouletteKey.KEY_LOCK.matches(event.getKey(), event.getScanCode())) {
            animationLocked = !animationLocked;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        LocalPlayer localPlayer;
        if (YesSteveModel.isAvailable() && event.phase == TickEvent.Phase.END && !animationLocked && (localPlayer = Minecraft.getInstance().player) != null && isPlayerMoving(localPlayer)) {
            localPlayer.getCapability(PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap -> {
                if (cap.isModelSwitching()) {
                    cap.clearModelSwitch();
                    if (NetworkHandler.isClientConnected()) {
                        NetworkHandler.sendToServer(C2SPlayAnimationPacket.createDefault());
                    }
                }
            });
        }
    }

    public static boolean isPlayerMoving(LocalPlayer localPlayer) {
        Input input = localPlayer.input;
        return input != null && (isSignificantImpulse(input.leftImpulse) || isSignificantImpulse(input.forwardImpulse) || input.jumping || input.shiftKeyDown);
    }

    private static boolean isSignificantImpulse(float impulse) {
        return Math.abs(impulse) > 1.0E-5f;
    }

    public static void toggleLock() {
        animationLocked = !animationLocked;
    }

    public static boolean isLocked() {
        return animationLocked;
    }
}
