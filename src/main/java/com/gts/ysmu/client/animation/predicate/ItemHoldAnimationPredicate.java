package com.gts.ysmu.client.animation.predicate;

import com.gts.ysmu.client.animation.IAnimationPredicate;
import com.gts.ysmu.client.animation.condition.ConditionManager;
import com.gts.ysmu.client.entity.LivingAnimatable;
import com.gts.ysmu.geckolib3.core.builder.ILoopType;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.client.entity.IPreviewAnimatable;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;
import com.gts.ysmu.client.animation.condition.ConditionSwing;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.StringUtils;

public class ItemHoldAnimationPredicate implements IAnimationPredicate<LivingAnimatable<?>> {
    @Override
    public PlayState predicate(AnimationEvent<LivingAnimatable<?>> event, ExpressionEvaluator<?> evaluator) {
        LivingEntity livingEntity = event.getAnimatable().getEntity();
        if (livingEntity == null || (event.getAnimatable() instanceof IPreviewAnimatable)) {
            return PlayState.STOP;
        }
        int i = event.getAnimatable().getModelAssembly().getModelData().getFormatVersion();
        if (livingEntity.swinging && !livingEntity.isSleeping()) {
            if (livingEntity.swingTime == 0 && ((LivingAnimatable) event.getAnimatable()).getPositionTracker().markProcessed(1)) {
                event.getController().stopTransition();
            }
            ConditionManager conditionManager = event.getAnimatable().getModelConfig();
            ConditionSwing conditionSwing = livingEntity.swingingArm == InteractionHand.MAIN_HAND ? conditionManager.getSwingMainhand() : conditionManager.getSwingOffhand();
            if (conditionSwing != null) {
                String str2 = conditionSwing.doTest(livingEntity, livingEntity.swingingArm);
                if (StringUtils.isNoneBlank(str2)) {
                    return IAnimationPredicate.playAnimationWithValid(event, str2, ILoopType.EDefaultLoopTypes.PLAY_ONCE, i);
                }
            }
            return IAnimationPredicate.playAnimationWithValid(event, livingEntity.swingingArm == InteractionHand.MAIN_HAND ? "swing_hand" : "swing_offhand", ILoopType.EDefaultLoopTypes.PLAY_ONCE, i);
        }
        return PlayState.CONTINUE;
    }
}
