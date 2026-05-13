package com.gts.ysmu.geckolib3.core.molang.builtin.math;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.ContextFunction;
import com.gts.ysmu.molang.runtime.ExecutionContext;

public class RandomInteger extends ContextFunction<Object> {
    @Override
    public boolean validateArgumentSize(int size) {
        return size == 2;
    }

    @Override
    protected Object eval(ExecutionContext<IContext<Object>> context, ArgumentCollection arguments) {
        int min = arguments.getAsInt(context, 0);
        int range = arguments.getAsInt(context, 1);
        if(min > range) {
            int temp = min;
            min = range;
            range = temp - range;
        } else {
            range -= min;
        }
        return min + context.entity().random().nextInt(range);
    }
}
