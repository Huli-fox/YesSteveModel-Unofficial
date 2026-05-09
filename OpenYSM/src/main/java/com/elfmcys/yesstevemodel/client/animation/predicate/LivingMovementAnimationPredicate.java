package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.animation.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.client.entity.IPreviewAnimatable;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionVehicle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.vehicle.Boat;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class LivingMovementAnimationPredicate implements IAnimationPredicate<LivingAnimatable<?>> {
    @Override
    public PlayState predicate(AnimationEvent<LivingAnimatable<?>> event, ExpressionEvaluator<?> evaluator) {
        return Objects.requireNonNullElse(renderRidingAnimation(event), PlayState.STOP);
    }

    @Nullable
    public PlayState renderRidingAnimation(AnimationEvent<LivingAnimatable<?>> event) {
        Entity vehicle;
        LivingEntity livingEntity = event.getAnimatable().getEntity();
        if (livingEntity == null || (event.getAnimatable() instanceof IPreviewAnimatable) || (vehicle = livingEntity.getVehicle()) == null || !vehicle.isAlive()) {
            return null;
        }
        ConditionManager conditionManager = event.getAnimatable().getModelConfig();
        ConditionVehicle conditionVehicle = conditionManager.getVehicle();
        if (conditionVehicle != null) {
            String str3 = conditionVehicle.doTest(livingEntity);
            if (StringUtils.isNoneBlank(str3)) {
                return IAnimationPredicate.playAnimationWithLoop(event, str3, ILoopType.EDefaultLoopTypes.LOOP);
            }
        }
        if (vehicle instanceof Pig) {
            return IAnimationPredicate.playAnimationWithLoop(event, "ride_pig", ILoopType.EDefaultLoopTypes.LOOP);
        }
        if (vehicle instanceof Saddleable) {
            return IAnimationPredicate.playAnimationWithLoop(event, "ride", ILoopType.EDefaultLoopTypes.LOOP);
        }
        if (vehicle instanceof Boat) {
            return IAnimationPredicate.playAnimationWithLoop(event, "boat", ILoopType.EDefaultLoopTypes.LOOP);
        }
        return IAnimationPredicate.playAnimationWithLoop(event, "sit", ILoopType.EDefaultLoopTypes.LOOP);
    }
}
