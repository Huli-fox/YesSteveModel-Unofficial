package com.gts.ysmu.client.animation.predicate;

import com.gts.ysmu.client.animation.IAnimationPredicate;
import com.gts.ysmu.client.entity.LivingAnimatable;
import com.gts.ysmu.client.animation.condition.ConditionArmor;
import com.gts.ysmu.geckolib3.core.builder.ILoopType;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.client.entity.IPreviewAnimatable;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.StringUtils;

public class ArmorPredicate implements IAnimationPredicate<LivingAnimatable<?>> {

    private final EquipmentSlot slot;

    public ArmorPredicate(EquipmentSlot slot) {
        this.slot = slot;
    }

    @Override
    public PlayState predicate(AnimationEvent<LivingAnimatable<?>> event, ExpressionEvaluator<?> evaluator) {
        LivingEntity entity = event.getAnimatable().getEntity();
        if (entity == null || (event.getAnimatable() instanceof IPreviewAnimatable)) {
            return PlayState.STOP;
        }
        if (entity.getItemBySlot(this.slot).isEmpty()) {
            return PlayState.STOP;
        }
        ConditionArmor conditionArmor = event.getAnimatable().getModelConfig().getArmor();
        if (conditionArmor != null) {
            String name = conditionArmor.doTest(entity, this.slot);
            if (StringUtils.isNoneBlank(name)) {
                return IAnimationPredicate.playAnimationWithLoop(event, name, ILoopType.EDefaultLoopTypes.LOOP);
            }
        }
        String defaultName = this.slot.getName() + ":default";
        if (event.getAnimatable().getAnimation(defaultName) != null) {
            return IAnimationPredicate.playAnimationWithLoop(event, defaultName, ILoopType.EDefaultLoopTypes.LOOP);
        }
        return PlayState.STOP;
    }
}
