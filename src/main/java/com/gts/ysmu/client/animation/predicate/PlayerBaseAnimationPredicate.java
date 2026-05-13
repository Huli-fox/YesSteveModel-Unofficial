package com.gts.ysmu.client.animation.predicate;

import com.gts.ysmu.client.animation.IAnimationPredicate;
import com.gts.ysmu.client.entity.CustomPlayerEntity;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.client.entity.IPreviewAnimatable;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;

public class PlayerBaseAnimationPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @Override
    public PlayState predicate(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        CustomPlayerEntity playerEntity = event.getAnimatable();
        if (playerEntity instanceof IPreviewAnimatable previewAnimatable) {
            if (previewAnimatable.getAnimationStateMachine().hasAnimation()) {
                return IAnimationPredicate.playLoopAnimation(event, previewAnimatable.getAnimationStateMachine().getCurrentAnimation());
            }
            return PlayState.STOP;
        }
        if (playerEntity.isModelSwitching()) {
            if (playerEntity.isDisabledState()) {
                playerEntity.enableModel();
                event.getController().stopTransition();
            }
            return IAnimationPredicate.predicate(event, playerEntity.getSelectedModelId());
        }
        return PlayState.STOP;
    }
}
