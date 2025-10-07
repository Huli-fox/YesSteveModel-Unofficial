package software.bernie.geckolib3.particles.components.appearance;

import com.eliotlash.molang.MolangException;
import com.eliotlash.molang.MolangParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.Tessellator;
import software.bernie.geckolib3.particles.BedrockSchemeJsonAdapter;
import software.bernie.geckolib3.particles.components.BedrockComponentBase;
import software.bernie.geckolib3.particles.components.IComponentParticleRender;
import software.bernie.geckolib3.particles.emitter.BedrockEmitter;
import software.bernie.geckolib3.particles.emitter.BedrockParticle;


public class BedrockComponentAppearanceTinting extends BedrockComponentBase implements IComponentParticleRender {
    public Tint color = new Tint.Solid();

    @Override
    public BedrockComponentBase fromJson(JsonElement elem, MolangParser parser) throws MolangException {
        if (!elem.isJsonObject()) return super.fromJson(elem, parser);

        JsonObject element = elem.getAsJsonObject();

        if (element.has("color")) {
            JsonElement color = element.get("color");

            if (color.isJsonArray() || color.isJsonPrimitive()) {
                this.color = Tint.parseColor(color, parser);
            } else if (color.isJsonObject()) {
                this.color = Tint.parseGradient(color.getAsJsonObject(), parser);
            }
        }

        return super.fromJson(element, parser);
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        JsonElement element = this.color.toJson();

        if (!BedrockSchemeJsonAdapter.isEmpty(element)) {
            object.add("color", element);
        }

        return object;
    }

    /* Interface implementations */

    @Override
    public void preRender(BedrockEmitter emitter, float partialTicks) {
    }

    @Override
    public void render(BedrockEmitter emitter, BedrockParticle particle, Tessellator builder, float partialTicks) {
        this.renderOnScreen(particle, 0, 0, 0, 0);
    }

    @Override
    public void renderOnScreen(BedrockParticle particle, int x, int y, float scale, float partialTicks) {
        if (this.color != null) {
            this.color.compute(particle);
        } else {
            particle.r = particle.g = particle.b = particle.a = 1;
        }
    }

    @Override
    public void postRender(BedrockEmitter emitter, float partialTicks) {
    }

    @Override
    public int getSortingIndex() {
        return -10;
    }
}
