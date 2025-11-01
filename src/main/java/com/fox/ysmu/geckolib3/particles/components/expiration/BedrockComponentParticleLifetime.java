package com.fox.ysmu.geckolib3.particles.components.expiration;

import com.fox.ysmu.geckolib3.core.molang.MolangException;
import com.fox.ysmu.geckolib3.core.molang.MolangParser;
import com.fox.ysmu.geckolib3.core.molang.expressions.MolangExpression;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.fox.ysmu.geckolib3.particles.components.BedrockComponentBase;
import com.fox.ysmu.geckolib3.particles.components.IComponentParticleInitialize;
import com.fox.ysmu.geckolib3.particles.components.IComponentParticleUpdate;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockEmitter;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockParticle;

public class BedrockComponentParticleLifetime extends BedrockComponentBase implements IComponentParticleInitialize, IComponentParticleUpdate {
    public MolangExpression expression = MolangParser.ZERO;
    public boolean max;

    @Override
    public BedrockComponentBase fromJson(JsonElement elem, MolangParser parser) throws MolangException {
        if (!elem.isJsonObject()) return super.fromJson(elem, parser);

        JsonObject element = elem.getAsJsonObject();
        JsonElement expression = null;

        if (element.has("expiration_expression")) {
            expression = element.get("expiration_expression");
            this.max = false;
        } else if (element.has("max_lifetime")) {
            expression = element.get("max_lifetime");
            this.max = true;
        } else {
            throw new JsonParseException("No expiration_expression or max_lifetime was found in minecraft:particle_lifetime_expression component");
        }

        this.expression = parser.parseJson(expression);

        return super.fromJson(element, parser);
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();

        object.add(this.max ? "max_lifetime" : "expiration_expression", this.expression.toJson());

        return object;
    }

    @Override
    public void update(BedrockEmitter emitter, BedrockParticle particle) {
        if (!this.max && this.expression.get() != 0) {
            particle.dead = true;
        }
    }

    @Override
    public void apply(BedrockEmitter emitter, BedrockParticle particle) {
        if (this.max) {
            particle.lifetime = (int) (this.expression.get() * 20);
        } else {
            particle.lifetime = -1;
        }
    }
}
