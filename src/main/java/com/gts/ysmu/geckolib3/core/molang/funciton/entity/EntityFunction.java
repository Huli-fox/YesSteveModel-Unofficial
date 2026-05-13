package com.gts.ysmu.geckolib3.core.molang.funciton.entity;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.ContextFunction;
import net.minecraft.world.entity.Entity;

public abstract class EntityFunction extends ContextFunction<Entity> {
    @Override
    public boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Entity;
    }
}
