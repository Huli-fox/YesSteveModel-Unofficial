package com.gts.ysmu.geckolib3.core.molang.builtin.math;

import com.gts.ysmu.molang.runtime.ExecutionContext;
import com.gts.ysmu.molang.runtime.Function;
import net.minecraft.util.Mth;

public class Trunc implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        float value = arguments.getAsFloat(context, 0);
        return value < 0 ? Mth.ceil(value) : Mth.floor(value);
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
