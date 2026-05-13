package com.gts.ysmu.geckolib3.core.molang.funciton.entity;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.ContextFunction;
import net.minecraft.world.entity.projectile.Projectile;

public abstract class AbstractProjectileFunction extends ContextFunction<Projectile> {
    @Override
    public boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Projectile;
    }
}
