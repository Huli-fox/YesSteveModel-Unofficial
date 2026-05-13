package com.gts.ysmu.client.animation.molang.functions.ctrl;

import com.gts.ysmu.geckolib3.core.controller.PredicateBasedController;
import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.ContextFunction;
import com.gts.ysmu.molang.runtime.ExecutionContext;

public class SetTransitionSpeed extends ContextFunction<Object> {
    @Override
    public Object eval(ExecutionContext<IContext<Object>> context, ArgumentCollection arguments) {
        PredicateBasedController<?> animationController = context.entity().animationEvent().getController();
        if (animationController == null) {
            return null;
        }
        float second = arguments.getAsFloat(context, 0);
        if (second < 0.0f) {
            return null;
        }
        animationController.setTransitionLengthTicks(second);
        return null;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
