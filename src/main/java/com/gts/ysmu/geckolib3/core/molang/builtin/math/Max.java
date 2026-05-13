package com.gts.ysmu.geckolib3.core.molang.builtin.math;

import com.gts.ysmu.molang.runtime.ExecutionContext;
import com.gts.ysmu.molang.runtime.Function;

public class Max implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        return Math.max(arguments.getAsFloat(context, 0),
                arguments.getAsFloat(context, 1));
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 2;
    }
}
