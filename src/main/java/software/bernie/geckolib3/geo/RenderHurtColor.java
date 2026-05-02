package software.bernie.geckolib3.geo;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;

import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;

/**
 * Applies the vanilla 1.7.10 hurt/death red overlay to Geo models.
 */
public final class RenderHurtColor {

    private RenderHurtColor() {}

    public static boolean set(EntityLivingBase entity, float partialTicks) {
        if (!shouldRender(entity)) {
            return false;
        }

        float brightness = entity.getBrightness(partialTicks);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthFunc(GL11.GL_EQUAL);
        GL11.glColor4f(brightness, 0.0F, 0.0F, 0.4F);
        return true;
    }

    public static void unset() {
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static boolean render(IGeoRenderer renderer, GeoModel model, Object animatable,
        EntityLivingBase entity, float partialTicks) {
        if (!set(entity, partialTicks)) {
            return false;
        }

        float brightness = entity.getBrightness(partialTicks);
        try {
            renderer.renderEarly(model, animatable, partialTicks, brightness, 0.0F, 0.0F, 0.4F);
            try {
                renderer.renderLate(model, animatable, partialTicks, brightness, 0.0F, 0.0F, 0.4F);
                Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawing(GL11.GL_QUADS);
                for (GeoBone group : model.topLevelBones) {
                    renderer.renderRecursively(tessellator, animatable, group, brightness, 0.0F, 0.0F, 0.4F);
                }
                tessellator.draw();
            } finally {
                renderer.renderAfter(model, animatable, partialTicks, brightness, 0.0F, 0.0F, 0.4F);
            }
        } finally {
            unset();
        }

        return true;
    }

    public static boolean shouldRender(EntityLivingBase entity) {
        return entity.hurtTime > 0 || entity.deathTime > 0;
    }

}
