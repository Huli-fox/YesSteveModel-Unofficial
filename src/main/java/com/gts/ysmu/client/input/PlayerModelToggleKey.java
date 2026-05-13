package com.gts.ysmu.client.input;

import com.gts.ysmu.YesSteveModel;
import com.gts.ysmu.client.gui.DisclaimerScreen;
import com.gts.ysmu.client.gui.ExtraPlayerConfigScreen;
import com.gts.ysmu.client.gui.PlayerModelScreen;
import com.gts.ysmu.config.GeneralConfig;
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
public class PlayerModelToggleKey {

    public static final KeyMapping KEY_MAPPING = new KeyMapping("key.yes_steve_model.player_model.desc", KeyConflictContext.IN_GAME, KeyModifier.ALT, InputConstants.Type.KEYSYM, 89, "key.category.yes_steve_model");

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (InputUtil.isPlayerReady() && event.getAction() == 1 && InputUtil.isKeyPressed(event, KEY_MAPPING)) {
            if (!YesSteveModel.isAvailable()) {
                YesSteveModel.sendUnavailableMessage();
                return;
            }
            if (NetworkHandler.isClientConnected() && !ServerConfig.CAN_SWITCH_MODEL.get()) {
                Minecraft.getInstance().setScreen(new ExtraPlayerConfigScreen(null));
            } else if (GeneralConfig.DISCLAIMER_SHOW.get()) {
                Minecraft.getInstance().setScreen(new DisclaimerScreen());
            } else {
                Minecraft.getInstance().setScreen(new PlayerModelScreen());
            }
        }
    }
}
