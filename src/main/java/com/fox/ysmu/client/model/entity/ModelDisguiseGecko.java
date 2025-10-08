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
            EntityModelData extraData = (EntityModelData) animationEvent.getExtraDataOfType(EntityModelData.class).get(0);
            head.setRotationX(extraData.headPitch * ((float) Math.PI / 180F));
            head.setRotationY(extraData.netHeadYaw * ((float) Math.PI / 180F));
        }
    }
}