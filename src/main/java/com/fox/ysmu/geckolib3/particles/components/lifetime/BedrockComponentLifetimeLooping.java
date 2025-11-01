package com.fox.ysmu.geckolib3.particles.components.lifetime;

import com.fox.ysmu.geckolib3.core.molang.MolangException;
import com.fox.ysmu.geckolib3.core.molang.MolangParser;
import com.fox.ysmu.geckolib3.core.molang.expressions.MolangExpression;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.fox.ysmu.geckolib3.particles.components.BedrockComponentBase;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockEmitter;

public class BedrockComponentLifetimeLooping extends BedrockComponentLifetime {
    public MolangExpression sleepTime = MolangParser.ZERO;

    public BedrockComponentBase fromJson(JsonElement elem, MolangParser parser) throws MolangException {
        if (!elem.isJsonObject()) {
            return super.fromJson(elem, parser);
        }

        JsonObject element = elem.getAsJsonObject();

        if (element.has("sleep_time")) {
            this.sleepTime = parser.parseJson(element.get("sleep_time"));
        }

        return super.fromJson(element, parser);
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = (JsonObject) super.toJson();

        if (!MolangExpression.isZero(this.sleepTime)) {
            object.add("sleep_time", this.sleepTime.toJson());
        }

        return object;
    }

    @Override
    public void update(BedrockEmitter emitter) {
        double active = this.activeTime.get();
        double sleep = this.sleepTime.get();
        double age = emitter.getAge();

        emitter.lifetime = (int) (active * 20);

        if (age >= active && emitter.playing) {
            emitter.stop();
        }

        if (age >= sleep && !emitter.playing && !emitter.lastLoop) {
            emitter.start();
        }
    }
}
