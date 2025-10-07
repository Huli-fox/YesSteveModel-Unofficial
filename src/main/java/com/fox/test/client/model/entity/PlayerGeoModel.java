package com.fox.test.client.model.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedTickingGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;
import com.fox.test.entity.PlayerWrapper;
import software.bernie.geckolib3.geo.render.built.GeoModel;

public class PlayerGeoModel extends AnimatedTickingGeoModel<PlayerWrapper> {

    @Override
    public ResourceLocation getModelLocation(PlayerWrapper entity) {
        return new ResourceLocation("test", "geo/main.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerWrapper entity) {
        return new ResourceLocation("test", "textures/model/entity/main.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(PlayerWrapper entity) {
        return new ResourceLocation("test", "animations/main.animation.json");
    }

    @Override
    public void setLivingAnimations(PlayerWrapper entity, Integer uniqueID, AnimationEvent animationEvent) {
        super.setLivingAnimations(entity, uniqueID, animationEvent);
        
        IBone head = this.getAnimationProcessor().getBone("Head");

        EntityModelData extraData = (EntityModelData) animationEvent.getExtraDataOfType(EntityModelData.class).get(0);
        if (head != null) {
            head.setRotationX(extraData.headPitch * ((float) Math.PI / 180F));
            head.setRotationY(extraData.netHeadYaw * ((float) Math.PI / 180F));
        }
    }
}