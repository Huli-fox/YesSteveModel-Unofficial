package com.gts.ysmu.client.animation.molang.functions.ysm;

import com.gts.ysmu.geckolib3.core.controller.AnimationControllerContext;
import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.ContextFunction;
import com.gts.ysmu.geckolib3.core.molang.util.StringPool;
import com.gts.ysmu.molang.runtime.ExecutionContext;

public class Defer extends ContextFunction<Object> {
    @Override
    public Object eval(ExecutionContext<IContext<Object>> context, ArgumentCollection arguments) {
        AnimationControllerContext animationControllerContext;
        int i;
        if (context.entity().isClientSide() && (animationControllerContext = context.entity().animationControllerContext()) != null && (i = arguments.getStringId(context, 0)) != StringPool.EMPTY_ID) {
            animationControllerContext.captureArguments(context, i, arguments, 1);
            return null;
        }
        return null;
    }
}
