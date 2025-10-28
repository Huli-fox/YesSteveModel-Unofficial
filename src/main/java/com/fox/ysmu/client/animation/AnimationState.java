package com.fox.ysmu.client.animation;

import com.fox.ysmu.client.entity.CustomPlayerEntity;
import com.fox.ysmu.geckolib3.core.builder.ILoopType;
import com.fox.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiPredicate;

public class AnimationState {
    private final String animationName;
    private final ILoopType loopType;
    private final int priority;
    private final BiPredicate<Player, AnimationEvent<CustomPlayerEntity>> predicate;

    public AnimationState(String animationName, ILoopType loopType, int priority, BiPredicate<Player, AnimationEvent<CustomPlayerEntity>> predicate) {
        this.animationName = animationName;
        this.loopType = loopType;
        this.priority = Mth.clamp(priority, Priority.HIGHEST, Priority.LOWEST);
        this.predicate = predicate;
    }

    public BiPredicate<Player, AnimationEvent<CustomPlayerEntity>> getPredicate() {
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
