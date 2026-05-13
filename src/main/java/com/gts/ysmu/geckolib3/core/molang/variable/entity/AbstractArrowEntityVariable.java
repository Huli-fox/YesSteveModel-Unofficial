package com.gts.ysmu.geckolib3.core.molang.variable.entity;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.variable.IValueEvaluator;
import com.gts.ysmu.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.world.entity.projectile.AbstractArrow;

public class AbstractArrowEntityVariable extends LambdaVariable<AbstractArrow> {
    public AbstractArrowEntityVariable(IValueEvaluator<?, IContext<AbstractArrow>> evaluator) {
        super(evaluator);
    }

    @Override
    public boolean validateContext(IContext<?> context) {
        return context.entity() instanceof AbstractArrow;
    }
}
