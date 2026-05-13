package com.gts.ysmu.client.animation.predicate;

import com.gts.ysmu.client.animation.IAnimationPredicate;
import com.gts.ysmu.client.entity.CustomPlayerEntity;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.client.entity.IPreviewAnimatable;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;
import org.apache.commons.lang3.StringUtils;

public class PlayerIdleAnimationPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @Override
    public PlayState predicate(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        String str = ((IPreviewAnimatable) event.getAnimatable()).getAnimationStateMachine().getQueuedAnimation();
        if (StringUtils.isNoneBlank(str)) {
            return IAnimationPredicate.playLoopAnimation(event, str);
        }
        return PlayState.STOP;
    }
}
