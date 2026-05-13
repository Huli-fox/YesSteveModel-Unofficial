package com.gts.ysmu.geckolib3.core.molang.builtin.math;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.ContextFunction;
import com.gts.ysmu.molang.runtime.ExecutionContext;
import net.minecraft.util.RandomSource;

public class DieRoll extends ContextFunction<Object> {
    @Override
    public boolean validateArgumentSize(int size) {
        return size == 3;
    }

    @Override
    protected Object eval(ExecutionContext<IContext<Object>> context, ArgumentCollection arguments) {
        int i = arguments.getAsInt(context, 0);
        float min = arguments.getAsFloat(context, 1);
        float range = arguments.getAsFloat(context, 2);
        if(min > range) {
            float temp = min;
            min = range;
            range = temp - range;
        } else {
            range -= min;
        }
        float total = 0;
        RandomSource rnd = context.entity().random();
        while (i-- > 0) {
            total += min + rnd.nextFloat() * range;
        }
        return total;
    }
}
