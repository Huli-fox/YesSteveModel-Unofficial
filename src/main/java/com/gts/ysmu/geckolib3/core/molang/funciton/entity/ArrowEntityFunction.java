package com.gts.ysmu.geckolib3.core.molang.funciton.entity;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.ContextFunction;
import net.minecraft.world.entity.projectile.Arrow;

public abstract class ArrowEntityFunction extends ContextFunction<Arrow> {
    @Override
    public boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Arrow;
    }
}
