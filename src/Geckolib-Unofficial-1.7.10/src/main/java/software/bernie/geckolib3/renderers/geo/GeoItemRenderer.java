package software.bernie.geckolib3.renderers.geo;

import com.eliotlash.mclib.utils.MatrixUtils;
import net.geckominecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimatableModel;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.example.config.ConfigHandler;

import javax.vecmath.Matrix4d;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3d;
import java.util.Collections;
import java.util.Objects;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class GeoItemRenderer<T extends Item & IAnimatable> implements IGeoRenderer<T>, IItemRenderer {
    // Register a model fetcher for this renderer
    static {
        AnimationController.addModelFetcher((IAnimatable object) -> {
            if (object instanceof Item) {
                Item item = (Item) object;
                IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(new ItemStack(item), ItemRenderType.INVENTORY);
                if (renderer instanceof GeoItemRenderer) {
                    return (IAnimatableModel<Object>) ((GeoItemRenderer<?>) renderer).getGeoModelProvider();
                }
            }
            return null;
        });
    }

    protected AnimatedGeoModel<T> modelProvider;
    protected ItemStack currentItemStack;

    public GeoItemRenderer(AnimatedGeoModel<T> modelProvider) {
        this.modelProvider = modelProvider;
    }

    public void setModel(AnimatedGeoModel<T> model) {
        this.modelProvider = model;
    }

    @Override
    public AnimatedGeoModel<T> getGeoModelProvider() {
        return modelProvider;
    }

    @Override
    public void renderItem(ItemRenderType var1, ItemStack itemStack, Object... var3) {
        GL11.glPushMatrix();
        try {

            if (var1 == ItemRenderType.INVENTORY) {
                GL11.glTranslated(-1, -1, 0);
                GL11.glRotatef(90, 0, 1, 0);

            }
            if (var1 != ItemRenderType.EQUIPPED_FIRST_PERSON) {
                GL11.glTranslated(0, -0.5, 0);
            }
            if (var1 == ItemRenderType.ENTITY) {
//            Matrix4f matrix4f;
//            PositionUtils.setInitialWorldPos();
//            GL11.glTranslated(2,4.5,-4);
//            Vector3d vec = PositionUtils.getCurrentRenderPos();
//            double out=0;
            }
            this.render((T) itemStack.getItem(), itemStack);
        } catch (Exception e) {
            if (ConfigHandler.debugPrintStacktraces) {
                e.printStackTrace();
            }
        } finally {
            GL11.glPopMatrix();
        }
    }

    public Vector3d getCurrentRenderPos() {
        Entity camera = Minecraft.getMinecraft().renderViewEntity;
        Matrix4f matrix4f = getCurrentMatrix();
        MatrixUtils.Transformation transformation = MatrixUtils.extractTransformations(null, matrix4f);
        double dl = matrix4f.m03;
        double du = matrix4f.m13;
        double dz = matrix4f.m23;

        Matrix4d rotMatrixX = new Matrix4d();
        rotMatrixX.rotX((camera.rotationPitch) / 360 * Math.PI * 2);
        Matrix4d rotMatrixY = new Matrix4d();
        rotMatrixY.rotY((-camera.rotationYaw) / 360 * Math.PI * 2);
        Vector3d vecZ = new Vector3d(0, 0, 1);
        rotMatrixX.transform(vecZ);
        rotMatrixY.transform(vecZ);

        Vector3d vecL = new Vector3d(1, 0, 0);
        rotMatrixX.transform(vecL);
        rotMatrixY.transform(vecL);
        vecZ.scale(-1);

        Vector3d vecU = new Vector3d(0, 1, 0);
        rotMatrixX.transform(vecU);
        rotMatrixY.transform(vecU);

        vecZ.scale(dz);
        vecU.scale(du);
        vecL.scale(-dl);
        Vector3d pos = new Vector3d(vecZ.x + vecU.x + vecL.x, vecZ.y + vecU.y + vecL.y, vecZ.z + vecU.z + vecL.z);
        Vector3d res = new Vector3d(pos.x + camera.posX, pos.y + camera.posY, pos.z + camera.posZ);
        return res;
    }

    public Matrix4f getCurrentMatrix() {
        MatrixUtils.matrix = null;
        MatrixUtils.captureMatrix();
        return MatrixUtils.matrix;
    }

    @Override
    public boolean handleRenderType(ItemStack is, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType var1, ItemStack var2, ItemRendererHelper var3) {
        return true;
    }

    public void render(T animatable, ItemStack itemStack) {
        this.currentItemStack = itemStack;
        GeoModel model = modelProvider.getModel(modelProvider.getModelLocation(animatable));
        AnimationEvent itemEvent = new AnimationEvent(animatable, 0, 0,
            Minecraft.getMinecraft().timer.renderPartialTicks, false, Collections.singletonList(itemStack));
        modelProvider.setLivingAnimations(animatable, this.getUniqueID(animatable), itemEvent);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(0, 0.01f, 0);
            GlStateManager.translate(0.5, 0.5, 0.5);

        Minecraft.getMinecraft().renderEngine.bindTexture(getTextureLocation(animatable));
        Color renderColor = getRenderColor(animatable, 0f);
        GL11.glRotatef(90, 0, 1, 0);
        render(model, animatable, 0, (float) renderColor.getRed() / 255f, (float) renderColor.getGreen() / 255f,
            (float) renderColor.getBlue() / 255f, (float) renderColor.getAlpha() / 255);
        } catch (Exception e) {
            if (ConfigHandler.debugPrintStacktraces) {
                e.printStackTrace();
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T instance) {
        return this.modelProvider.getTextureLocation(instance);
    }

    @Override
    public Integer getUniqueID(T animatable) {
        return Objects.hash(currentItemStack.getItem(), currentItemStack.stackSize,
            currentItemStack.hasTagCompound() ? currentItemStack.getTagCompound().toString() : 1);
    }
}
