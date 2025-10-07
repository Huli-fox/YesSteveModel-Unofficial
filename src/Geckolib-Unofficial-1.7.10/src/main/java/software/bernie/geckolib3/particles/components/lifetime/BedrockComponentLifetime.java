package software.bernie.geckolib3.particles.components.lifetime;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.molang.MolangException;
import com.eliotlash.molang.MolangParser;
import com.eliotlash.molang.expressions.MolangExpression;
import com.eliotlash.molang.expressions.MolangValue;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import software.bernie.geckolib3.particles.components.BedrockComponentBase;
import software.bernie.geckolib3.particles.components.IComponentEmitterUpdate;

public abstract class BedrockComponentLifetime extends BedrockComponentBase implements IComponentEmitterUpdate {
    public static final MolangExpression DEFAULT_ACTIVE = new MolangValue(null, new Constant(10));

    public MolangExpression activeTime = DEFAULT_ACTIVE;

    public BedrockComponentBase fromJson(JsonElement elem, MolangParser parser) throws MolangException {
        if (!elem.isJsonObject()) {
            return super.fromJson(elem, parser);
        }

        JsonObject element = elem.getAsJsonObject();

        if (element.has(this.getPropertyName())) {
            this.activeTime = parser.parseJson(element.get(this.getPropertyName()));
        }

        return super.fromJson(element, parser);
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();

        if (!MolangExpression.isConstant(this.activeTime, 10)) {
            object.add(this.getPropertyName(), this.activeTime.toJson());
        }

        return object;
    }

    protected String getPropertyName() {
        return "active_time";
    }

    @Override
    public int getSortingIndex() {
        return -10;
    }
}
