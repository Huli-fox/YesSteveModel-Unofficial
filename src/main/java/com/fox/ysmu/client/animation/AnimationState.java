package com.fox.ysmu.client.animation;

import java.util.function.BiPredicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import com.fox.ysmu.client.entity.CustomPlayerEntity;

import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;

public class AnimationState {

    private final String animationName;
    private final ILoopType loopType;
    private final int priority;
    private final BiPredicate<EntityPlayer, AnimationEvent<CustomPlayerEntity>> predicate;

    public AnimationState(String animationName, ILoopType loopType, int priority,
        BiPredicate<EntityPlayer, AnimationEvent<CustomPlayerEntity>> predicate) {
        this.animationName = animationName;
        this.loopType = loopType;
        this.priority = MathHelper.clamp_int(priority, Priority.HIGHEST, Priority.LOWEST);
        this.predicate = predicate;
    }

    public BiPredicate<EntityPlayer, AnimationEvent<CustomPlayerEntity>> getPredicate() {
        return predicate;
    }

    public String getAnimationName() {
        return animationName;
    }

    public ILoopType getLoopType() {
        return loopType;
    }

    public int getPriority() {
        return priority;
    }
}
