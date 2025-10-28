package com.fox.ysmu.client.event;

import com.fox.ysmu.ysmu;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.client.entity.CustomPlayerEntity;
import com.fox.ysmu.client.renderer.CustomPlayerRenderer;
import com.fox.ysmu.Config;
import com.fox.ysmu.event.api.SpecialPlayerRenderEvent;
import com.fox.ysmu.geckolib3.core.IAnimatable;
import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.fox.ysmu.geckolib3.resource.GeckoLibCache;
import com.fox.ysmu.util.AnimatableCacheUtil;
import com.fox.ysmu.util.ModelIdUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ExecutionException;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ysmu.MODID)
public class RenderFirstPlayerBackground {
    private static final String NAME = "Background";
    /**
     * 因为 RenderHandEvent 可有几率会渲染多次，所以为了避免多次渲染，这样设计
     */
    private static boolean ALREADY_RENDERED = false;

    @SubscribeEvent
    public static void onRenderLevelLase(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            ALREADY_RENDERED = false;
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (Config.DISABLE_SELF_MODEL) {
            return;
        }
        if (Config.DISABLE_SELF_HANDS) {
            return;
        }
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null || ALREADY_RENDERED) {
            return;
        }
        ALREADY_RENDERED = true;
        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep != null){
            ResourceLocation modelId = eep.getModelId();
            GeoModel geoModel = GeckoLibCache.getInstance().getGeoModels().get(ModelIdUtil.getArmId(eep.getModelId()));
            if (geoModel == null || !geoModel.hasTopLevelBone(NAME)) {
                return;
            }
            CustomPlayerRenderer instance = RegisterEntityRenderersEvent.getInstance();
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource multiBufferSource = event.getMultiBufferSource();
            VertexConsumer buffer;
            IAnimatable animatable;

            try {
                animatable = AnimatableCacheUtil.ANIMATABLE_CACHE.get(modelId, () -> {
                    CustomPlayerEntity entity = new CustomPlayerEntity();
                    entity.setTexture(eep.getSelectTexture());
                    return entity;
                });
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }

            if (animatable instanceof CustomPlayerEntity customPlayer) {
                customPlayer.setTexture(eep.getSelectTexture());
                if (MinecraftForge.EVENT_BUS.post(new SpecialPlayerRenderEvent(player, customPlayer, modelId))) {
                    return;
                }
                buffer = multiBufferSource.getBuffer(RenderType.entityTranslucent(customPlayer.getTexture()));
                int packedLight = event.getPackedLight();
                if (instance != null) {
                    poseStack.pushPose();
                    if (Minecraft.getInstance().options.bobView().get()) {
                        bobView(poseStack, event.getPartialTick(), player);
                    }
                    poseStack.translate(0, -1.5, 0);
                    geoModel.getTopLevelBone(NAME).ifPresent(bone -> instance.renderRecursively(bone, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1));
                    poseStack.popPose();
                }
            }
        }
    }

    private static void bobView(PoseStack pMatrixStack, float pPartialTicks, Player player) {
        float walk = player.walkDist - player.walkDistO;
        float walk2 = -(player.walkDist + walk * pPartialTicks);
        float lerp = Mth.lerp(pPartialTicks, player.oBob, player.bob);
        pMatrixStack.translate(-Mth.sin(walk2 * (float) Math.PI) * lerp * 0.5F, Math.abs(Mth.cos(walk2 * (float) Math.PI) * lerp), 0.0D);
        pMatrixStack.mulPose(Axis.ZN.rotationDegrees(Mth.sin(walk2 * (float) Math.PI) * lerp * 3.0F));
        pMatrixStack.mulPose(Axis.XN.rotationDegrees(Math.abs(Mth.cos(walk2 * (float) Math.PI - 0.2F) * lerp) * 5.0F));
    }
}
