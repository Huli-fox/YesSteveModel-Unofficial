package com.gts.ysmu.client.input;

import com.gts.ysmu.YesSteveModel;
import com.gts.ysmu.capability.PlayerCapabilityProvider;
import com.gts.ysmu.resource.models.ModelProperties;
import com.gts.ysmu.client.event.AnimationLockEvent;
import com.gts.ysmu.client.gui.AnimationRouletteScreen;
import com.gts.ysmu.client.model.ModelAssembly;
import com.gts.ysmu.geckolib3.core.molang.util.StringPool;
import com.gts.ysmu.network.NetworkHandler;
import com.gts.ysmu.network.message.C2SPlayAnimationPacket;
import com.gts.ysmu.util.InputUtil;
import com.gts.ysmu.util.data.OrderedStringMap;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class ExtraAnimationKey {

    public static final List<KeyMapping> KEY_MAPPINGS = Lists.newArrayList();

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        for (int i = 0; i <= 7; i++) {
            KeyMapping eventMapping = new KeyMapping(String.format("key.yes_steve_model.extra_animation.%d.desc", Integer.valueOf(i)), KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, -1, "key.category.yes_steve_model");
            event.register(eventMapping);
            KEY_MAPPINGS.add(eventMapping);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!YesSteveModel.isAvailable() || !InputUtil.isPlayerReady()) {
            return;
        }
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        for (KeyMapping eventMapping : KEY_MAPPINGS) {
            if (event.getAction() == 1 && InputUtil.isKeyPressed(event, eventMapping) && localPlayer != null && !AnimationLockEvent.isPlayerMoving(localPlayer)) {
                localPlayer.getCapability(PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap -> {
                    ModelAssembly modelAssembly = cap.getModelAssembly();
                    int index = KEY_MAPPINGS.indexOf(eventMapping);
                    ModelProperties modelProperties = modelAssembly.getModelData().getModelProperties();
                    OrderedStringMap<String, String> map = modelProperties.getExtraAnimation();
                    if (map.size() > index) {
                        String rouletteKey = map.getKeyAt(index);
                        if ("#return".equals(rouletteKey)) {
                            NetworkHandler.sendToServer(C2SPlayAnimationPacket.createDefault());
                            return;
                        }
                        if (rouletteKey.startsWith("#") && modelProperties.getExtraAnimationClassify().containsKey(rouletteKey.substring(1))) {
                            AnimationRouletteScreen.setInitialSubmenu(rouletteKey.substring(1));
                            Minecraft.getInstance().setScreen(new AnimationRouletteScreen(modelProperties.getExtraAnimationButtons(), modelProperties.getExtraAnimationClassify(), modelAssembly, cap));
                            return;
                        }
                        NetworkHandler.sendToServer(new C2SPlayAnimationPacket(index, StringPool.EMPTY));
                    }
                });
                return;
            }
        }
    }
}
