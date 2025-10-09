package com.fox.ysmu.client.model.entity;

import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedTickingGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;
import com.fox.ysmu.entity.EntityDisguiseGecko;

public class ModelDisguiseGecko extends AnimatedTickingGeoModel<EntityDisguiseGecko> {

    // 你的modid，必须和文件夹结构里的modid一致
    private static final String MOD_ID = "ysmu";

    // 动态获取模型路径
    @Override
    public ResourceLocation getModelLocation(EntityDisguiseGecko entity) {
        return new ResourceLocation(MOD_ID, "geo/" + entity.getModelName() + ".geo.json");
    }

    // 动态获取贴图路径
    @Override
    public ResourceLocation getTextureLocation(EntityDisguiseGecko entity) {
        return new ResourceLocation(MOD_ID, "textures/entity/" + entity.getModelName() + ".png");
    }

    // 动态获取动画文件路径
    @Override
    public ResourceLocation getAnimationFileLocation(EntityDisguiseGecko entity) {
        return new ResourceLocation(MOD_ID, "animations/" + entity.getModelName() + ".animation.json");
    }

    // 同步头部转动，让模型更生动
    @SuppressWarnings("unchecked")
    @Override
    public void setLivingAnimations(EntityDisguiseGecko entity, Integer uniqueID, AnimationEvent animationEvent) {
        super.setLivingAnimations(entity, uniqueID, animationEvent);
        IBone head = this.getAnimationProcessor().getBone("AllHead");

        if (head != null) {
            float partialTicks = animationEvent.getPartialTick();

            // 计算平滑的、插值后的【头部绝对朝向】和【身体绝对朝向】
            float interpHeadYaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
            float interpBodyYaw = entity.prevRenderYawOffset + (entity.renderYawOffset - entity.prevRenderYawOffset) * partialTicks;

            // 计算出平滑的、插值后的【头部相对身体的朝向】
            float interpNetHeadYaw = interpHeadYaw - interpBodyYaw;

            // 获取平滑的、插值后的【头部俯仰角】
            float interpPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;

            // 应用Y轴旋转（左右看），对Yaw取反以校正坐标系
            head.setRotationY(-interpNetHeadYaw * ((float) Math.PI / 180F));

            // 应用X轴旋转（上下看），同样对Pitch取反以校正坐标系
            head.setRotationX(-interpPitch * ((float) Math.PI / 180F));
        }
    }
}