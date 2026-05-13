package com.gts.ysmu.geckolib3.core.molang.variable.entity;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.variable.IValueEvaluator;
import com.gts.ysmu.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.world.entity.projectile.Projectile;

public class ProjectileEntityVariable extends LambdaVariable<Projectile> {
    public ProjectileEntityVariable(IValueEvaluator<?, IContext<Projectile>> evaluator) {
        super(evaluator);
    }

    @Override
    public boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Projectile;
    }
}
