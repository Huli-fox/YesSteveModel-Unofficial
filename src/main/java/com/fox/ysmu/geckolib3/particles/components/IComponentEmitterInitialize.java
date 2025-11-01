package com.fox.ysmu.geckolib3.particles.components;

import com.fox.ysmu.geckolib3.particles.emitter.BedrockEmitter;

public interface IComponentEmitterInitialize extends IComponentBase {
    public void apply(BedrockEmitter emitter);
}
