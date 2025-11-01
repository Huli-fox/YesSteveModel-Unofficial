package com.fox.ysmu.geckolib3.particles.components.motion;

import com.fox.ysmu.geckolib3.core.molang.MolangException;
import com.fox.ysmu.geckolib3.core.molang.MolangParser;
import com.fox.ysmu.geckolib3.core.molang.expressions.MolangExpression;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.fox.ysmu.geckolib3.particles.components.BedrockComponentBase;
import com.fox.ysmu.geckolib3.particles.components.IComponentParticleInitialize;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockEmitter;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockParticle;

public class BedrockComponentInitialSpin extends BedrockComponentBase implements IComponentParticleInitialize {
    public MolangExpression rotation = MolangParser.ZERO;
    public MolangExpression rate = MolangParser.ZERO;

    @Override
    public BedrockComponentBase fromJson(JsonElement elem, MolangParser parser) throws MolangException {
        if (!elem.isJsonObject()) return super.fromJson(elem, parser);

        JsonObject element = elem.getAsJsonObject();

        if (element.has("rotation")) this.rotation = parser.parseJson(element.get("rotation"));
        if (element.has("rotation_rate")) this.rate = parser.parseJson(element.get("rotation_rate"));

        return super.fromJson(element, parser);
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();

        if (!MolangExpression.isZero(this.rotation)) object.add("rotation", this.rotation.toJson());
        if (!MolangExpression.isZero(this.rate)) object.add("rotation_rate", this.rate.toJson());

        return object;
    }

    @Override
    public void apply(BedrockEmitter emitter, BedrockParticle particle) {
        particle.initialRotation = (float) this.rotation.get();
        particle.rotationVelocity = (float) this.rate.get() / 20;
    }
}
