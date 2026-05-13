package com.gts.ysmu.geckolib3.core.molang.builtin.math;

import com.gts.ysmu.molang.runtime.ExecutionContext;
import com.gts.ysmu.molang.runtime.Function;

public class MinAngle implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        float angle = arguments.getAsFloat(context, 0) % 360.0f;
        if (angle >= 180.0f) {
            return angle - 360.0f;
        } else if (angle < -180.0f) {
            return angle + 360.0f;
        } else {
            return angle;
        }
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
