package com.gts.ysmu.client.animation.predicate;

import com.gts.ysmu.client.animation.IAnimationPredicate;
import com.gts.ysmu.geckolib3.core.AnimatableEntity;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;

public class NamedAnimationPredicate<T extends AnimatableEntity<?>> implements IAnimationPredicate<T> {

    private final String animationName;

    public NamedAnimationPredicate(String animationName) {
        this.animationName = animationName;
    }

    @Override
    public PlayState predicate(AnimationEvent<T> event, ExpressionEvaluator<?> evaluator) {
        return IAnimationPredicate.playLoopAnimation(event, this.animationName);
    }
}
