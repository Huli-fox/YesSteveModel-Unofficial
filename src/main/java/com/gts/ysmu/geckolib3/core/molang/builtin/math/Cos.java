package com.gts.ysmu.geckolib3.core.molang.builtin.math;

import com.gts.ysmu.molang.runtime.ExecutionContext;
import com.gts.ysmu.molang.runtime.Function;
import net.minecraft.util.Mth;

public class Cos implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        return Mth.cos(arguments.getAsFloat(context, 0) / 180.0f * 3.1415927f);
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
