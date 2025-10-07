package software.bernie.geckolib3.particles.components.lifetime;

import com.eliotlash.mclib.math.Operation;
import com.eliotlash.molang.MolangException;
import com.eliotlash.molang.MolangParser;
import com.eliotlash.molang.expressions.MolangExpression;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import software.bernie.geckolib3.particles.components.BedrockComponentBase;
import software.bernie.geckolib3.particles.emitter.BedrockEmitter;

public class BedrockComponentLifetimeExpression extends BedrockComponentLifetime {
    public MolangExpression expiration = MolangParser.ZERO;

    @Override
    protected String getPropertyName() {
        return "activation_expression";
    }

    public BedrockComponentBase fromJson(JsonElement elem, MolangParser parser) throws MolangException {
        if (!elem.isJsonObject()) {
            return super.fromJson(elem, parser);
        }

        JsonObject element = elem.getAsJsonObject();

        if (element.has("expiration_expression")) {
            this.expiration = parser.parseJson(element.get("expiration_expression"));
        }

        return super.fromJson(element, parser);
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = (JsonObject) super.toJson();

        if (!MolangExpression.isZero(this.expiration)) {
            object.add("expiration_expression", this.expiration.toJson());
        }

        return object;
    }

    @Override
    public void update(BedrockEmitter emitter) {
        if (!Operation.equals(this.activeTime.get(), 0)) {
            emitter.start();
        }

        if (!Operation.equals(this.expiration.get(), 0)) {
            emitter.stop();
        }
    }
}
