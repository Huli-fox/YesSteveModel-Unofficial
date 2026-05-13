package com.gts.ysmu.geckolib3.core.molang.value;

import com.gts.ysmu.molang.parser.ast.Expression;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;

import java.util.List;

public class MolangValue implements IValue {

    private final List<Expression> expressions;

    private final boolean isScript;

    public MolangValue(List<Expression> list, boolean isScript) {
        this.expressions = list;
        this.isScript = isScript;
    }

    @Override
    public Object evalUnsafe(ExpressionEvaluator<?> evaluator) {
        return evaluator.evalAll(this.expressions, this.isScript);
    }
}
