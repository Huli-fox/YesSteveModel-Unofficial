package software.bernie.geckolib3.particles.components;

import com.eliotlash.molang.MolangException;
import com.eliotlash.molang.MolangParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class BedrockComponentBase {
    public BedrockComponentBase fromJson(JsonElement element, MolangParser parser) throws MolangException {
        return this;
    }

    public JsonElement toJson() {
        return new JsonObject();
    }

    public boolean canBeEmpty() {
        return false;
    }
}
