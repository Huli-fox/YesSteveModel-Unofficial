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
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ExecutionException;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ysmu.MODID)
public class ReplacePlayerHandRenderEvent {
    private static final String LEFT_ARM = "LeftArm";
    private static final String RIGHT_ARM = "RightArm";

    @SubscribeEvent
    public static void onRenderHand(RenderArmEvent event) {
        if (Config.DISABLE_SELF_MODEL) {
            return;
        }
        if (Config.DISABLE_SELF_HANDS) {
            return;
        }
        event.setCanceled(true);
        AbstractClientPlayer player = event.getPlayer();
        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep != null){
            ResourceLocation modelId = eep.getModelId();
            GeoModel geoModel = GeckoLibCache.getInstance().getGeoModels().get(ModelIdUtil.getArmId(eep.getModelId()));
            if (geoModel == null || !hasArmBone(event.getArm(), geoModel)) {
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
                    if (event.getArm() == HumanoidArm.LEFT) {
                        poseStack.pushPose();
                        poseStack.translate(0.25, 1.8, 0);
                        poseStack.scale(-1, -1, 1);
                        geoModel.getTopLevelBone(LEFT_ARM).ifPresent(bone -> instance.renderRecursively(bone, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1));
                        poseStack.popPose();
                    }
                    if (event.getArm() == HumanoidArm.RIGHT) {
                        poseStack.pushPose();
                        poseStack.translate(-0.25, 1.8, 0);
                        poseStack.scale(-1, -1, 1);
                        geoModel.getTopLevelBone(RIGHT_ARM).ifPresent(bone -> instance.renderRecursively(bone, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1));
                        poseStack.popPose();
                    }
                }
            }
        }
    }

    private static boolean hasArmBone(HumanoidArm arm, GeoModel model) {
        if (arm == HumanoidArm.LEFT) {
            return model.hasTopLevelBone(LEFT_ARM);
        } else {
            return model.hasTopLevelBone(RIGHT_ARM);
        }
    }
}
