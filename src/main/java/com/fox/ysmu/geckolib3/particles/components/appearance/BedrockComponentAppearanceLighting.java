package com.fox.ysmu.geckolib3.particles.components.appearance;

import com.fox.ysmu.geckolib3.particles.components.BedrockComponentBase;
import com.fox.ysmu.geckolib3.particles.components.IComponentEmitterInitialize;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockEmitter;

public class BedrockComponentAppearanceLighting extends BedrockComponentBase implements IComponentEmitterInitialize {
    @Override
    public void apply(BedrockEmitter emitter) {
        emitter.lit = false;
    }

    @Override
    public boolean canBeEmpty() {
        return true;
    }
}
