package com.fox.ysmu.geckolib3.particles.components;

import net.minecraft.client.renderer.Tessellator;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockEmitter;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockParticle;

public interface IComponentRenderBase extends IComponentBase {
    public void render(BedrockEmitter emitter, BedrockParticle particle, Tessellator builder, float partialTicks);

    public void renderOnScreen(BedrockParticle particle, int x, int y, float scale, float partialTicks);

    public void preRender(BedrockEmitter emitter, float partialTicks);

    public void postRender(BedrockEmitter emitter, float partialTicks);
}
