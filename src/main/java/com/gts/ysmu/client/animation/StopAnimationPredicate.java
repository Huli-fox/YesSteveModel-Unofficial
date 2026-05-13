package com.gts.ysmu.client.animation;

import com.gts.ysmu.geckolib3.core.AnimatableEntity;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.LivingEntity;

public class StopAnimationPredicate implements IAnimationPredicate<AnimatableEntity<? extends LivingEntity>> {

    public static final StopAnimationPredicate INSTANCE = new StopAnimationPredicate();

    @Override
    public PlayState predicate(AnimationEvent<AnimatableEntity<? extends LivingEntity>> event, ExpressionEvaluator<?> evaluator) {
        return PlayState.STOP;
    }
}
