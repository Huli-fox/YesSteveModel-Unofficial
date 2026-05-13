package com.gts.ysmu.client.model;

import com.gts.ysmu.geckolib3.core.controller.controllers.VehicleAnimationController;
import com.gts.ysmu.geckolib3.geo.render.built.GeoModel;
import com.gts.ysmu.geckolib3.core.builder.AnimationController;
import com.gts.ysmu.client.model.ModelResourceBundle;
import com.gts.ysmu.client.entity.GeckoVehicleEntity;
import com.gts.ysmu.geckolib3.core.builder.Animation;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.function.Consumer;

public class VehicleModelBundle {
    private final GeoModel model;

    private final Object2ReferenceMap<String, Animation> animations;

    private final Object2ReferenceMap<String, AnimationController> animationControllers;

    private final AbstractTexture texture;

    private final Consumer<GeckoVehicleEntity> controllerInitializer;

    public VehicleModelBundle(GeoModel model, Object2ReferenceMap<String, Animation> animations, Object2ReferenceMap<String, AnimationController> animationControllers, AbstractTexture abstractTexture, ModelResourceBundle modelResourceBundle) {
        this.model = model;
        this.animations = animations;
        this.animationControllers = animationControllers;
        this.texture = abstractTexture;
        this.controllerInitializer = VehicleAnimationController.buildControllers(this, modelResourceBundle);
    }

    public GeoModel getModel() {
        return this.model;
    }

    public Object2ReferenceMap<String, Animation> getAnimations() {
        return this.animations;
    }

    public Object2ReferenceMap<String, AnimationController> getAnimationControllers() {
        return this.animationControllers;
    }

    public AbstractTexture getTexture() {
        return this.texture;
    }

    public Consumer<GeckoVehicleEntity> getAnimatableConsumer() {
        return this.controllerInitializer;
    }
}
