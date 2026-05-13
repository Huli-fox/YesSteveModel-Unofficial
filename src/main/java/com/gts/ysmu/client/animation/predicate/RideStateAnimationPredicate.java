package com.gts.ysmu.client.animation.predicate;

import com.gts.ysmu.client.animation.IAnimationPredicate;
import com.gts.ysmu.client.entity.GeckoVehicleEntity;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.Entity;

public class RideStateAnimationPredicate implements IAnimationPredicate<GeckoVehicleEntity> {

    public static final String[] ANIMATION_NAMES = {"has_ride", "not_ride"};

    @Override
    public PlayState predicate(AnimationEvent<GeckoVehicleEntity> event, ExpressionEvaluator<?> evaluator) {
        Entity entity = event.getAnimatable().getEntity();
        if (entity == null) {
            return PlayState.STOP;
        }
        if (!entity.getPassengers().isEmpty()) {
            return IAnimationPredicate.predicate(event, "has_ride");
        }
        return IAnimationPredicate.predicate(event, "not_ride");
    }
}
