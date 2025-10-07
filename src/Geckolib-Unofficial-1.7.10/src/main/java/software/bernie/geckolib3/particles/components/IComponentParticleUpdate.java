package software.bernie.geckolib3.particles.components;

import software.bernie.geckolib3.particles.emitter.BedrockEmitter;
import software.bernie.geckolib3.particles.emitter.BedrockParticle;

public interface IComponentParticleUpdate extends IComponentBase {
    public void update(BedrockEmitter emitter, BedrockParticle particle);
}
