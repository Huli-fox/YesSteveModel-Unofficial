package software.bernie.geckolib3.geo;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

/**
 * This class is a hack to access protected setBrightness() and
 * unsetBrightness() methods in order to apply red tint on the models upon
 * entities getting hurt
 */
public class RenderHurtColor extends RendererLivingEntity {
    /**
     * Private instance
     */
    private static RenderHurtColor instance;

    public static RenderHurtColor getInstance() {
        if (instance == null) {
            instance = new RenderHurtColor(null, 0);
        }

        return instance;
    }

    public static boolean set(EntityLivingBase entity, float partialTicks) {
        return true;//getInstance().setBrightness(entity, partialTicks, true); // TODO
    }

    public static void unset() {
        //getInstance().unsetBrightness();
    }

    public RenderHurtColor(ModelBase modelBaseIn, float shadowSizeIn) {
        super(modelBaseIn, shadowSizeIn);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
