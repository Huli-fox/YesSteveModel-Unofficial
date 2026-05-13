package com.gts.ysmu.client.animation;

import com.gts.ysmu.client.entity.CustomPlayerEntity;
import com.gts.ysmu.client.entity.IPreviewAnimatable;
import com.gts.ysmu.geckolib3.core.builder.ILoopType;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class AnimationManager implements IAnimationPredicate<CustomPlayerEntity> {

    private static final ReferenceArrayList<AnimationState<Player, CustomPlayerEntity>>[] data = new ReferenceArrayList[Priority.LOWEST + 1];

    static {
        for (int i = 0; i < data.length; i++) {
            data[i] = new ReferenceArrayList<>(6);
        }
    }

    public static void register(AnimationState<Player, CustomPlayerEntity> state) {
        data[state.getPriority()].add(state);
    }

    @Override
    public PlayState predicate(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        Player player = event.getAnimatable().getEntity();
        if (player == null) {
            return PlayState.STOP;
        }
        if (event.getAnimatable() instanceof IPreviewAnimatable) {
            return PlayState.STOP;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle != null && vehicle.isAlive()) {
            return PlayState.STOP;
        }
        for (int i = Priority.HIGHEST; i <= Priority.LOWEST; i++) {
            for (AnimationState<Player, CustomPlayerEntity> animationState : data[i]) {
                if (animationState.getPredicate().test(player, event)) {
                    String name = animationState.getAnimationName();
                    ILoopType loopType = animationState.getLoopType();
                    return IAnimationPredicate.playAnimationWithLoop(event, name, loopType);
                }
            }
        }
        return PlayState.STOP;
    }
}
