package com.fox.ysmu.client.model.entity;

import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedTickingGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;
import com.fox.ysmu.entity.EntityDisguiseGecko;

import java.util.List;

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
        // 调用父类方法，确保基础动画逻辑（例如动画控制器更新）正常执行
        super.setLivingAnimations(entity, uniqueID, animationEvent);

        // 从动画处理器中获取模型部位
        IBone head = this.getAnimationProcessor().getBone("AllHead");
        if (head == null) return; // 没找到则直接返回，防止空指针

        // 定义头部俯仰角（Pitch）和相对身体的水平转角（Yaw）
        float headPitch;
        float netHeadYaw;

        // 从动画事件中尝试获取附加的模型数据（一般包含玩家视角朝向信息）
        List<EntityModelData> extra = animationEvent.getExtraDataOfType(EntityModelData.class);
        if (extra != null && !extra.isEmpty()) {
            // 如果有附加数据，就直接用它来更新头部角度
            EntityModelData d = extra.get(0);
            headPitch = d.headPitch;     // 俯仰角（上下）
            netHeadYaw = d.netHeadYaw;   // 左右相对转角
        } else {
            // 如果没有附加数据，就手动计算插值后的朝向（使动画平滑）

            float pt = animationEvent.getPartialTick(); // partialTicks，用于帧间插值

            // 插值后的身体朝向（renderYawOffset 控制身体转向）
            float body = entity.prevRenderYawOffset + (entity.renderYawOffset - entity.prevRenderYawOffset) * pt;

            // 插值后的头部朝向（rotationYawHead 控制头部方向）
            float headYaw = entity.prevRotationYawHead + (entity.rotationYawHead - entity.prevRotationYawHead) * pt;

            // 计算头部相对于身体的水平偏转角
            netHeadYaw = wrapDegrees(headYaw - body);

            // 限制头部左右转动角度（防止过度旋转）
            if (netHeadYaw > 85.0F) netHeadYaw = 85.0F;
            if (netHeadYaw < -85.0F) netHeadYaw = -85.0F;

            // 插值后的俯仰角（上下看角度）
            headPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * pt;
        }

        // 将角度转为弧度并应用到骨骼上
        head.setRotationX((float) Math.toRadians(headPitch));   // 头部上下旋转
        head.setRotationY((float) Math.toRadians(netHeadYaw));  // 头部左右旋转
    }

    /**
     * 角度标准化工具方法：
     * 将角度限制在 (-180°, 180°) 区间内，防止数值累积导致旋转异常。
     */
    private float wrapDegrees(float angle) {
        angle = angle % 360.0F;
        if (angle >= 180.0F) angle -= 360.0F;
        if (angle < -180.0F) angle += 360.0F;
        return angle;
    }
}
