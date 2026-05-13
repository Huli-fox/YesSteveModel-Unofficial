package com.gts.ysmu.geckolib3.core.molang.variable.entity;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.variable.IValueEvaluator;
import com.gts.ysmu.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.world.entity.Entity;

public class EntityVariable extends LambdaVariable<Entity> {
    public EntityVariable(IValueEvaluator<?, IContext<Entity>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Entity;
    }
}
