package com.gts.ysmu.client.animation.predicate;

import com.gts.ysmu.client.animation.IAnimationPredicate;
import com.gts.ysmu.client.entity.GeckoProjectileEntity;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;
import com.gts.ysmu.util.accessors.ProjectileStateAccessor;
import net.minecraft.world.entity.projectile.Projectile;

public class ProjectileAnimationPredicate implements IAnimationPredicate<GeckoProjectileEntity> {

    public static final String[] ENVIRONMENT_STATES = {"water", "ground", "fly", "fire"};

    @Override
    public PlayState predicate(AnimationEvent<GeckoProjectileEntity> event, ExpressionEvaluator<?> evaluator) {
        Projectile projectile = event.getAnimatable().getEntity();
        if (projectile == null) {
            return PlayState.STOP;
        }
        if (projectile.isInWater()) {
            return IAnimationPredicate.predicate(event, "water");
        }
        if (projectile.isOnFire()) {
            return IAnimationPredicate.predicate(event, "fire");
        }
        if ((projectile instanceof ProjectileStateAccessor) && ((ProjectileStateAccessor) projectile).isInGround()) {
            return IAnimationPredicate.predicate(event, "ground");
        }
        return IAnimationPredicate.predicate(event, "air");
    }
}
