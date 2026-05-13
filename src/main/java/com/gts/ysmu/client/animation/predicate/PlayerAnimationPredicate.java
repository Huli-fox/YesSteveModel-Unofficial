package com.gts.ysmu.client.animation.predicate;

import com.gts.ysmu.client.animation.IAnimationPredicate;
import com.gts.ysmu.client.entity.CustomPlayerEntity;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.client.entity.IPreviewAnimatable;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public class PlayerAnimationPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @Override
    public PlayState predicate(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        Player player = event.getAnimatable().getEntity();
        if (player == null || (event.getAnimatable() instanceof IPreviewAnimatable)) {
            return PlayState.STOP;
        }
        if (player.getPose() == Pose.SWIMMING) {
            return PlayState.STOP;
        }
        if (player.getPose() == Pose.FALL_FLYING && player.isFallFlying()) {
            return PlayState.STOP;
        }
        return PlayState.STOP;
    }
}
