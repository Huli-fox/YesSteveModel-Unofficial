package com.gts.ysmu.client.renderer.layer;

import com.gts.ysmu.client.entity.CustomPlayerEntity;
import com.gts.ysmu.geckolib3.geo.GeoLayerRenderer;
import com.gts.ysmu.geckolib3.geo.animated.AnimatedGeoModel;
import com.gts.ysmu.geckolib3.util.RenderUtils;
import com.gts.ysmu.util.accessors.BufferSourceAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CustomPlayerItemInHandLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private final ItemInHandRenderer itemRenderer;

    public CustomPlayerItemInHandLayer(ItemInHandRenderer itemInHandRenderer) {
        this.itemRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity entity = entityLivingBaseIn.getEntity();
        AnimatedGeoModel animatedGeoModel = entityLivingBaseIn.getCurrentModel();
        if (animatedGeoModel == null) {
            return;
        }
        ItemStack offhandItem = entity.getOffhandItem();
        ItemStack mainHandItem = entity.getMainHandItem();
        if (!offhandItem.isEmpty() || !mainHandItem.isEmpty()) {
            poseStack.pushPose();
            boolean useExtraPlayer = entityLivingBaseIn.isRenderLayersFirst();
            if (!animatedGeoModel.rightHandBones().isEmpty()) {
                renderItem(animatedGeoModel, entity, mainHandItem, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, bufferSource, packedLightIn);
                if (useExtraPlayer && !mainHandItem.isEmpty() && (bufferSource instanceof BufferSourceAccessor)) {
                    ((BufferSourceAccessor) bufferSource).initialize();
                }
            }
            if (!animatedGeoModel.leftHandBones().isEmpty()) {
                renderItem(animatedGeoModel, entity, offhandItem, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, bufferSource, packedLightIn);
                if (useExtraPlayer && !offhandItem.isEmpty() && (bufferSource instanceof BufferSourceAccessor)) {
                    ((BufferSourceAccessor) bufferSource).initialize();
                }
            }
            poseStack.popPose();
        }
    }

    public void renderItem(AnimatedGeoModel model, LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, HumanoidArm humanoidArm, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
        if (!itemStack.isEmpty()) {
            boolean isLeftHand = humanoidArm == HumanoidArm.LEFT;
            poseStack.pushPose();
            if (!applyItemBoneTransform(humanoidArm, poseStack, model)) {
                poseStack.translate(0.0d, -0.0625d, -0.1d);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0f));
                this.itemRenderer.renderItem(livingEntity, itemStack, itemDisplayContext, isLeftHand, poseStack, multiBufferSource, i);
            }
            poseStack.popPose();
            (isLeftHand ? model.rightHandChain() : model.leftHandChains()).forEach(list -> {
                poseStack.pushPose();
                if (!RenderUtils.prepMatrixForLocator(poseStack, list)) {
                    poseStack.translate(0.0d, -0.0625d, -0.1d);
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0f));
                    this.itemRenderer.renderItem(livingEntity, itemStack, itemDisplayContext, isLeftHand, poseStack, multiBufferSource, i);
                }
                poseStack.popPose();
            });
        }
    }

    public boolean applyItemBoneTransform(HumanoidArm humanoidArm, PoseStack poseStack, AnimatedGeoModel model) {
        if (humanoidArm == HumanoidArm.LEFT) {
            return RenderUtils.prepMatrixForLocator(poseStack, model.leftHandBones());
        }
        return RenderUtils.prepMatrixForLocator(poseStack, model.rightHandBones());
    }
}
