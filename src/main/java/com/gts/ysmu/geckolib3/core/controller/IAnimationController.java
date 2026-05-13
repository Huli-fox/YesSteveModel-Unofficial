package com.gts.ysmu.geckolib3.core.controller;

import com.gts.ysmu.geckolib3.core.AnimatableEntity;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.molang.value.IValue;
import com.gts.ysmu.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.gts.ysmu.geckolib3.core.molang.context.AnimationContext;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;

import java.util.List;
import java.util.function.Consumer;

public interface IAnimationController<T extends AnimatableEntity<?>> {
    String getName();

    String getCurrentAnimation();

    void init(List<BoneTopLevelSnapshot> list, Object2ReferenceMap<String, List<IValue>> object2ReferenceMap);

    void process(AnimationEvent<T> event, ExpressionEvaluator<AnimationContext<?>> evaluator, boolean z);

    void forEachTransform(Consumer<BoneTransformProvider> consumer);

    void reset();

    @Deprecated
    default boolean isDeprecatedMode() {
        return false;
    }
}
