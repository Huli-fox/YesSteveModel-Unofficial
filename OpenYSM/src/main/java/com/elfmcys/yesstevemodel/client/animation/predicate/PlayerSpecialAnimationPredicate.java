package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.animation.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.client.entity.IPreviewAnimatable;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.player.Player;

public class PlayerSpecialAnimationPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @Override
    public PlayState predicate(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        Player player = event.getAnimatable().getEntity();
        if (player == null || (event.getAnimatable() instanceof IPreviewAnimatable)) {
            return null;
        }
        return PlayState.STOP;
    }
}
