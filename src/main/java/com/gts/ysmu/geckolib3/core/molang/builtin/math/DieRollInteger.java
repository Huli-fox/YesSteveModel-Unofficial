package com.gts.ysmu.geckolib3.core.molang.builtin.math;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.ContextFunction;
import com.gts.ysmu.molang.runtime.ExecutionContext;
import net.minecraft.util.RandomSource;

public class DieRollInteger extends ContextFunction<Object> {
    @Override
    public boolean validateArgumentSize(int size) {
        return size == 3;
    }

    @Override
    protected Object eval(ExecutionContext<IContext<Object>> context, ArgumentCollection arguments) {
        int i = Math.round(arguments.getAsFloat(context, 0));
        int min = arguments.getAsInt(context, 1);
        int range = arguments.getAsInt(context, 2);
        if(min > range) {
            int temp = min;
            min = range;
            range = temp - range;
        } else {
            range -= min;
        }
        int total = 0;
        RandomSource rnd = context.entity().random();
        while (i-- > 0) {
            total += min + rnd.nextInt(range);
        }
        return total;
    }
}
