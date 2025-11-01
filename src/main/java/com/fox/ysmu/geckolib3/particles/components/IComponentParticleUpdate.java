package com.fox.ysmu.geckolib3.particles.components;

import com.fox.ysmu.geckolib3.particles.emitter.BedrockEmitter;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockParticle;

public interface IComponentParticleUpdate extends IComponentBase {
    public void update(BedrockEmitter emitter, BedrockParticle particle);
}
