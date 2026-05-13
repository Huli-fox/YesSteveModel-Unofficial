package com.gts.ysmu.geckolib3.core.molang.builtin.math;

import com.gts.ysmu.molang.runtime.ExecutionContext;
import com.gts.ysmu.molang.runtime.Function;
import net.minecraft.util.Mth;

public class Clamp implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        return Mth.clamp(arguments.getAsFloat(context, 0),
                arguments.getAsFloat(context, 1),
                arguments.getAsFloat(context, 2));
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 3;
    }
}
