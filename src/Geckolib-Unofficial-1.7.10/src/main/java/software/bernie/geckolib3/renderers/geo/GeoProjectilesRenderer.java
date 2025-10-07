package software.bernie.geckolib3.renderers.geo;

import net.geckominecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimatableModel;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.GeoModelProvider;
import software.bernie.geckolib3.model.provider.data.EntityModelData;
import software.bernie.geckolib3.util.AnimationUtils;
import software.bernie.example.config.ConfigHandler;

import java.util.Collections;

@SuppressWarnings("unchecked")
public class GeoProjectilesRenderer<T extends Entity & IAnimatable> extends Render implements IGeoRenderer<T> {

    static {
        AnimationController.addModelFetcher((IAnimatable object) -> {
            if (object instanceof Entity) {
                return (IAnimatableModel<Object>) AnimationUtils.getGeoModelForEntity((Entity) object);
            }
            return null;
        });
    }

    private final AnimatedGeoModel<T> modelProvider;

    public GeoProjectilesRenderer(RenderManager renderManager, AnimatedGeoModel<T> modelProvider) {
        super();
        this.modelProvider = modelProvider;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        try {
        GeoModel model = modelProvider.getModel(modelProvider.getModelLocation((T) entity));
        GlStateManager.rotate(
            entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks - 90.0F, 0.0F,
            1.0F, 0.0F);
        GlStateManager.rotate(
            entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, 0.0F, 0.0F,
            1.0F);

        float lastLimbDistance = 0.0F;
        float limbSwing = 0.0F;
        EntityModelData entityModelData = new EntityModelData();
        AnimationEvent<T> predicate = new AnimationEvent<T>((T) entity, limbSwing, lastLimbDistance, partialTicks,
            !(lastLimbDistance > -0.15F && lastLimbDistance < 0.15F), Collections.singletonList(entityModelData));
        if (modelProvider instanceof IAnimatableModel) {
            ((IAnimatableModel<T>) modelProvider).setLivingAnimations((T) entity, this.getUniqueID(entity), predicate);
        }
        GlStateManager.pushMatrix();
        try {
        Minecraft.getMinecraft().renderEngine.bindTexture(getTextureLocation(entity));
        Color renderColor = getRenderColor((T) entity, partialTicks);

        if (!entity.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer))
            render(model, (T) entity, partialTicks, (float) renderColor.getRed() / 255f,
                (float) renderColor.getGreen() / 255f, (float) renderColor.getBlue() / 255f,
                (float) renderColor.getAlpha() / 255);
        } catch (Exception e) {
            if (ConfigHandler.debugPrintStacktraces) {
                e.printStackTrace();
            }
        } finally {
            GlStateManager.popMatrix();
        }
        } catch (Exception e) {
            if (ConfigHandler.debugPrintStacktraces) {
                e.printStackTrace();
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @Override
    public GeoModelProvider<T> getGeoModelProvider() {
        return this.modelProvider;
    }

    @Override
    public ResourceLocation getEntityTexture(Entity instance) {
        return getTextureLocation(instance);
    }

    @Override
    public Integer getUniqueID(Entity animatable) {
        return animatable.getUniqueID().hashCode();
    }

    @Override
    public ResourceLocation getTextureLocation(Entity instance) {
        return this.modelProvider.getTextureLocation((T) instance);
    }

}
