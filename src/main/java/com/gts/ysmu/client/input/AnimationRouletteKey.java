package com.gts.ysmu.client.input;

import com.gts.ysmu.YesSteveModel;
import com.gts.ysmu.capability.PlayerCapabilityProvider;
import com.gts.ysmu.client.gui.AnimationRouletteScreen;
import com.gts.ysmu.client.model.ModelAssembly;
import com.gts.ysmu.config.ServerConfig;
import com.gts.ysmu.network.NetworkHandler;
import com.gts.ysmu.util.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class AnimationRouletteKey {

    public static final KeyMapping KEY_ROULETTE = new KeyMapping("key.yes_steve_model.animation_roulette.desc", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, 90, "key.category.yes_steve_model");

    public static final KeyMapping KEY_LOCK = new KeyMapping("key.yes_steve_model.lock_roulette.desc", KeyConflictContext.IN_GAME, KeyModifier.ALT, InputConstants.Type.KEYSYM, 76, "key.category.yes_steve_model");

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && event.getAction() == 1 && InputUtil.isKeyPressed(event, KEY_ROULETTE)) {
            if (!NetworkHandler.isClientConnected() || ServerConfig.CAN_SWITCH_MODEL.get()) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.getCapability(PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap -> {
                        String modelId = cap.getModelId();
                        ModelAssembly modelAssembly = cap.getModelAssembly();
                        if (modelAssembly != null && !modelAssembly.getModelData().getModelProperties().getExtraAnimation().isEmpty()) {
                            if (Minecraft.getInstance().screen == null) {
                                Minecraft.getInstance().setScreen(new AnimationRouletteScreen(modelId, modelAssembly, cap));
                            } else if (Minecraft.getInstance().screen instanceof AnimationRouletteScreen) {
                                Minecraft.getInstance().setScreen(null);
                            }
                        }
                    });
                }
            }
        }
    }
}
