package com.fox.ysmu.geckolib3.model;

import com.fox.ysmu.geckolib3.core.IAnimatable;
import com.fox.ysmu.geckolib3.core.IAnimatableModel;
import com.fox.ysmu.geckolib3.core.builder.Animation;
import com.fox.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.fox.ysmu.geckolib3.core.manager.AnimationData;
import com.fox.ysmu.geckolib3.core.processor.AnimationProcessor;
import com.fox.ysmu.geckolib3.core.processor.IBone;
import com.fox.ysmu.geckolib3.file.AnimationFile;
import com.fox.ysmu.geckolib3.geo.exception.GeckoLibException;
import com.fox.ysmu.geckolib3.geo.render.built.GeoBone;
import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.fox.ysmu.geckolib3.model.provider.GeoModelProvider;
import com.fox.ysmu.geckolib3.model.provider.IAnimatableModelProvider;
import com.fox.ysmu.geckolib3.resource.GeckoLibCache;
import com.fox.ysmu.geckolib3.util.AnimationTicker;
import com.fox.ysmu.util.Keep;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import java.util.Collections;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AnimatedGeoModel<T extends IAnimatable> extends GeoModelProvider<T> implements IAnimatableModel<T>, IAnimatableModelProvider<T> {
    private final AnimationProcessor animationProcessor;
    private GeoModel currentModel;

    protected AnimatedGeoModel() {
        this.animationProcessor = new AnimationProcessor(this);
    }

    public void registerBone(GeoBone bone) {
        registerModelRenderer(bone);
        for (GeoBone childBone : bone.childBones) {
            registerBone(childBone);
        }
    }

    @Override
    @Keep
    public void setCustomAnimations(T animatable, int instanceId, AnimationEvent animationEvent) {
        Minecraft mc = Minecraft.getMinecraft();
        AnimationData manager = animatable.getFactory().getOrCreateAnimationData(instanceId);
        AnimationEvent<T> predicate;
        double currentTick = animatable instanceof Entity ? ((Entity) animatable).ticksExisted : getCurrentTick();
        float partialTicks = mc.timer.renderPartialTicks;
        if (manager.startTick == -1) {
            manager.startTick = currentTick + partialTicks;
        }

        if (!mc.isGamePaused() || manager.shouldPlayWhilePaused) {
            if (animatable instanceof EntityLivingBase) {
                manager.tick = currentTick + partialTicks;
                double gameTick = manager.tick;
                double deltaTicks = gameTick - this.lastGameTickTime;
                this.seekTime += deltaTicks;
                this.lastGameTickTime = gameTick;
                codeAnimations(animatable, instanceId, animationEvent);
            } else {
                manager.tick = currentTick - manager.startTick;
                double gameTick = manager.tick;
                double deltaTicks = gameTick - this.lastGameTickTime;
                this.seekTime += deltaTicks;
                this.lastGameTickTime = gameTick;
            }
        }

        predicate = animationEvent == null ? new AnimationEvent<T>(animatable, 0, 0, (float) (manager.tick - this.lastGameTickTime), false, Collections.emptyList()) : animationEvent;
        predicate.animationTick = this.seekTime;
        getAnimationProcessor().preAnimationSetup(predicate.getAnimatable(), this.seekTime);
        if (!getAnimationProcessor().getModelRendererList().isEmpty()) {
            getAnimationProcessor().tickAnimation(animatable, instanceId, this.seekTime, predicate, GeckoLibCache.getInstance().parser, this.shouldCrashOnMissing);
        }
    }

    public void codeAnimations(T entity, Integer uniqueID, AnimationEvent<?> customPredicate) {
    }

    @Override
    @Keep
    public AnimationProcessor getAnimationProcessor() {
        return this.animationProcessor;
    }

    public void registerModelRenderer(IBone modelRenderer) {
        this.animationProcessor.registerModelRenderer(modelRenderer);
    }

    @Override
    @Keep
    public Animation getAnimation(String name, IAnimatable animatable) {
        AnimationFile animation = GeckoLibCache.getInstance().getAnimations().get(this.getAnimationFileLocation((T) animatable));
        if (animation == null) {
            throw new GeckoLibException(this.getAnimationFileLocation((T) animatable), "Could not find animation file. Please double check name.");
        }
        return animation.getAnimation(name);
    }

    @Override
    @Keep
    public GeoModel getModel(ResourceLocation location) {
        GeoModel model = super.getModel(location);
        if (model == null) {
            throw new GeckoLibException(location, "Could not find model. If you are getting this with a built mod, please just restart your game.");
        }
        if (model != this.currentModel) {
            this.animationProcessor.clearModelRendererList();
            this.currentModel = model;
            for (GeoBone bone : model.topLevelBones) {
                registerBone(bone);
            }
        }
        return model;
    }

    public GeoModel getCurrentModel() {
        return currentModel;
    }

    @Override
    @Keep
    public double getCurrentTick() {
        return (Minecraft.getSystemTime() / 50d);
    }
}
