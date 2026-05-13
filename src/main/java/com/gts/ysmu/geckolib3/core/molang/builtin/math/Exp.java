package com.gts.ysmu.geckolib3.core.molang.builtin.math;

import com.gts.ysmu.molang.runtime.ExecutionContext;
import com.gts.ysmu.molang.runtime.Function;

public class Exp implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        return Math.exp(arguments.getAsDouble(context, 0));
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
