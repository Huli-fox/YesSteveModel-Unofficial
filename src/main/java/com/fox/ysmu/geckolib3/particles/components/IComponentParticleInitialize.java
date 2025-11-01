package com.fox.ysmu.geckolib3.particles.components;

import com.fox.ysmu.geckolib3.particles.emitter.BedrockEmitter;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockParticle;

public interface IComponentParticleInitialize extends IComponentBase {
    public void apply(BedrockEmitter emitter, BedrockParticle particle);
}
