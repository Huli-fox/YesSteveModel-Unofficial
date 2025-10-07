package com.fox.test.client.model.entity;

import com.fox.test.entity.CustomEntity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedTickingGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

public class CustomEntityModel extends AnimatedTickingGeoModel<CustomEntity> {

    // 返回动画文件的路径
    @Override
    public ResourceLocation getAnimationFileLocation(CustomEntity entity) {
        // "test" 是您的modid, "animations/main.animation.json" 是动画文件的路径
        return new ResourceLocation("test", "animations/main.animation.json");
    }

    // 返回模型文件的路径
    @Override
    public ResourceLocation getModelLocation(CustomEntity entity) {
        // "test" 是您的modid, "geo/main.geo.json" 是模型文件的路径
        return new ResourceLocation("test", "geo/main.geo.json");
    }

    // 返回纹理文件的路径
    @Override
    public ResourceLocation getTextureLocation(CustomEntity entity) {
        // "test" 是您的modid, "textures/model/entity/main.png" 是纹理文件的路径
        // ***注意***: 建议将 main.png 移动到 resources/assets/test/textures/model/entity/ 目录下以符合常规做法
        return new ResourceLocation("test", "textures/model/entity/main.png");
    }

    // 这个方法可以让你在每帧渲染前对模型的骨骼进行操作，例如让头部跟随玩家视线
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void setLivingAnimations(CustomEntity entity, Integer uniqueID, AnimationEvent animationEvent) {
        super.setLivingAnimations(entity, uniqueID, animationEvent);
        // 获取名为 "head" 的骨骼, 请确保您的模型中确实有名为 "head" 的骨骼
        IBone head = this.getAnimationProcessor().getBone("head");

        EntityModelData extraData = (EntityModelData) animationEvent.getExtraDataOfType(EntityModelData.class).get(0);
        if (head != null) {
            // 设置头部的旋转以匹配玩家的视线
            head.setRotationX(extraData.headPitch * ((float) Math.PI / 180F));
            head.setRotationY(extraData.netHeadYaw * ((float) Math.PI / 180F));
        }
    }
}