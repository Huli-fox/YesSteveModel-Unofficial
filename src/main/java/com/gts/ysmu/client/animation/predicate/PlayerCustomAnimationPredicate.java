package com.gts.ysmu.client.animation.predicate;

import com.gts.ysmu.client.animation.IAnimationPredicate;
import com.gts.ysmu.client.entity.CustomPlayerEntity;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.client.entity.IPreviewAnimatable;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;
import org.apache.commons.lang3.StringUtils;

public class PlayerCustomAnimationPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @Override
    public PlayState predicate(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        IPreviewAnimatable previewAnimatable = (IPreviewAnimatable) event.getAnimatable();
        String str = previewAnimatable.getAnimationStateMachine().getPreviousAnimation();
        if (StringUtils.isNoneBlank(str)) {
            previewAnimatable.setCustomAnimationActive(true);
            return IAnimationPredicate.playLoopAnimation(event, str);
        }
        previewAnimatable.setCustomAnimationActive(false);
        event.getController().markDirty();
        return PlayState.STOP;
    }
}
