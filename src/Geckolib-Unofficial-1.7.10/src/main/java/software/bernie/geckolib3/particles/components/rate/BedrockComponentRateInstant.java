package software.bernie.geckolib3.particles.components.rate;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.Operation;
import com.eliotlash.molang.MolangException;
import com.eliotlash.molang.MolangParser;
import com.eliotlash.molang.expressions.MolangExpression;
import com.eliotlash.molang.expressions.MolangValue;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import software.bernie.geckolib3.particles.components.BedrockComponentBase;
import software.bernie.geckolib3.particles.components.IComponentEmitterUpdate;
import software.bernie.geckolib3.particles.emitter.BedrockEmitter;

public class BedrockComponentRateInstant extends BedrockComponentRate implements IComponentEmitterUpdate {
    public static final MolangExpression DEFAULT_PARTICLES = new MolangValue(null, new Constant(10));

    public BedrockComponentRateInstant() {
        this.particles = DEFAULT_PARTICLES;
    }

    public BedrockComponentBase fromJson(JsonElement elem, MolangParser parser) throws MolangException {
        if (!elem.isJsonObject()) return super.fromJson(elem, parser);

        JsonObject element = elem.getAsJsonObject();

        if (element.has("num_particles")) {
            this.particles = parser.parseJson(element.get("num_particles"));
        }

        return super.fromJson(element, parser);
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();

        if (!MolangExpression.isConstant(this.particles, 10)) {
            object.add("num_particles", this.particles.toJson());
        }

        return object;
    }

    @Override
    public void update(BedrockEmitter emitter) {
        double age = emitter.getAge();

        if (emitter.playing && Operation.equals(age, 0)) {
            emitter.setEmitterVariables(0);

            for (int i = 0, c = (int) this.particles.get(); i < c; i++) {
                emitter.spawnParticle();
            }
        }
    }
}
