package com.gts.ysmu.client.animation.predicate;

import com.gts.ysmu.client.animation.IAnimationPredicate;
import com.gts.ysmu.client.entity.LivingAnimatable;
import com.gts.ysmu.geckolib3.core.builder.ILoopType;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.client.entity.IPreviewAnimatable;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;
import com.gts.ysmu.client.animation.condition.ConditionPassenger;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.StringUtils;

public class OffhandAttackAnimationPredicate implements IAnimationPredicate<LivingAnimatable<?>> {
    @Override
    public PlayState predicate(AnimationEvent<LivingAnimatable<?>> event, ExpressionEvaluator<?> evaluator) {
        LivingEntity livingEntity = (LivingEntity) ((LivingAnimatable) event.getAnimatable()).getEntity();
        if (livingEntity == null || (event.getAnimatable() instanceof IPreviewAnimatable)) {
            return PlayState.STOP;
        }
        Entity firstPassenger = livingEntity.getFirstPassenger();
        if (firstPassenger == null || !firstPassenger.isAlive()) {
            return PlayState.STOP;
        }
        ConditionPassenger conditionPassenger = event.getAnimatable().getModelConfig().getPassenger();
        if (conditionPassenger != null) {
            String str = conditionPassenger.doTest(livingEntity);
            if (StringUtils.isNoneBlank(str)) {
                return IAnimationPredicate.playAnimationWithLoop(event, str, ILoopType.EDefaultLoopTypes.LOOP);
            }
        }
        return PlayState.STOP;
    }
}
