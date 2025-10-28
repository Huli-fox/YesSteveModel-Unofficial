package com.fox.ysmu.client.event;

import com.fox.ysmu.ysmu;
import com.fox.ysmu.client.entity.CustomPlayerEntity;
import com.fox.ysmu.event.api.SpecialPlayerRenderEvent;
import com.fox.ysmu.util.ModelIdUtil;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ysmu.MODID)
public class VanillaPlayerRenderEvent {
    private static final ResourceLocation STEVE_SKIN_LOCATION = new ResourceLocation("textures/entity/player/wide/steve.png");
    private static final ResourceLocation ALEX_SKIN_LOCATION = new ResourceLocation("textures/entity/player/slim/alex.png");
    private static final String STEVE = "steve";
    private static final String ALEX = "alex";

    @SubscribeEvent
    public static void onRenderPlayer(SpecialPlayerRenderEvent event) {
        Player player = event.getPlayer();
        CustomPlayerEntity animatable = event.getCustomPlayer();
        if (isVanillaPlayer(event.getModelId()) && player instanceof AbstractClientPlayer clientPlayer) {
            animatable.setPlayer(player);
            animatable.setMainModel(ModelIdUtil.getMainId(event.getModelId()));
            ResourceLocation location;
            Minecraft minecraft = Minecraft.getInstance();
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager().getInsecureSkinInformation(clientPlayer.getGameProfile());
            if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                location = minecraft.getSkinManager().registerTexture(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
            } else {
                location = getDefaultSkin(event.getModelId());
            }
            animatable.setTexture(location);
        }
    }

    private static boolean isVanillaPlayer(ResourceLocation modelId) {
        return modelId.getPath().equals(STEVE) || modelId.getPath().equals(ALEX);
    }

    private static ResourceLocation getDefaultSkin(ResourceLocation modelId) {
        return modelId.getPath().equals(STEVE) ? STEVE_SKIN_LOCATION : ALEX_SKIN_LOCATION;
    }
}
