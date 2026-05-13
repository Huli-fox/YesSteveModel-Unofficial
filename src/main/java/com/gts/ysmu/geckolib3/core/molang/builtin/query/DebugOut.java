package com.gts.ysmu.geckolib3.core.molang.builtin.query;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.ContextFunction;
import com.gts.ysmu.molang.runtime.ExecutionContext;

public class DebugOut extends ContextFunction<Object> {
    @Override
    protected Object eval(ExecutionContext<IContext<Object>> context, ArgumentCollection arguments) {
        if (!context.entity().isDebugMode()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arguments.size(); i++) {
            Object value = arguments.getValue(context, i);
            sb.append(value == null ? "null" : value);
        }
        context.entity().logWarning(sb.toString());
        return null;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size > 0;
    }
}
