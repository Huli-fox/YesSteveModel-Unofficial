package com.gts.ysmu.geckolib3.core.molang.funciton.entity;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.ContextFunction;
import net.minecraft.client.player.LocalPlayer;

public abstract class LocalPlayerEntityFunction extends ContextFunction<LocalPlayer> {
    @Override
    public boolean validateContext(IContext<?> context) {
        return context.entity() instanceof LocalPlayer;
    }
}
