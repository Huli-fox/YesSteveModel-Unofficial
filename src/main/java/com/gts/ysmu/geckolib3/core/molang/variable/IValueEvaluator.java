package com.gts.ysmu.geckolib3.core.molang.variable;

public interface IValueEvaluator<TValue, TContext> {
    TValue eval(TContext ctx);
}
