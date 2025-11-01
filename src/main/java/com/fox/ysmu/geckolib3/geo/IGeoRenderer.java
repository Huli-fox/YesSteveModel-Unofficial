package com.fox.ysmu.geckolib3.geo;

import com.fox.ysmu.geckolib3.util.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import com.fox.ysmu.geckolib3.core.IAnimatable;
import com.fox.ysmu.geckolib3.core.controller.AnimationController;
import com.fox.ysmu.geckolib3.core.util.Color;
import com.fox.ysmu.geckolib3.geo.render.built.GeoBone;
import com.fox.ysmu.geckolib3.geo.render.built.GeoCube;
import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.fox.ysmu.geckolib3.geo.render.built.GeoQuad;
import com.fox.ysmu.geckolib3.geo.render.built.GeoVertex;
import com.fox.ysmu.geckolib3.model.provider.GeoModelProvider;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockEmitter;
import com.fox.ysmu.geckolib3.util.MatrixStack;
import com.fox.ysmu.geckolib3.util.ConfigHandler;
import com.fox.ysmu.geckolib3.util.PositionUtils;

//import javax.vecmath.Matrix3f;
//import javax.vecmath.Matrix4f;
//import javax.vecmath.Vector3d;
//import javax.vecmath.Vector3f;
//import javax.vecmath.Vector4f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Map;

public interface IGeoRenderer<T> {
    public static MatrixStack MATRIX_STACK = new MatrixStack();

    default void render(GeoModel model, T animatable, float partialTicks, float red, float green, float blue,
                        float alpha) {
        GlStateManager.disableCull();
        GlStateManager.enableRescaleNormal();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        renderEarly(model, animatable, partialTicks, red, green, blue, alpha);

        renderLate(model, animatable, partialTicks, red, green, blue, alpha);
        Tessellator tess = Tessellator.instance;
        //BufferBuilder builder = Tessellator.instance.getBuffer();

        //builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
        tess.startDrawing(GL11.GL_QUADS);//, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
        // Render all top level bones
        for (GeoBone group : model.topLevelBones) {
            renderRecursively(tess, animatable, group, red, green, blue, alpha);
        }

        Tessellator.instance.draw();

        renderAfter(model, animatable, partialTicks, red, green, blue, alpha);
        //GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
    }

    default boolean isBoneRenderOverriden(T animatable, GeoBone bone) {
        return false;
    }

    default void drawOverridenBone(T animatable, GeoBone bone) {

    }

    default void renderRecursively(Tessellator builder, T animatable, GeoBone bone, float red, float green, float blue,
                                   float alpha) {
        MATRIX_STACK.push();

        MATRIX_STACK.translate(bone);
        MATRIX_STACK.moveToPivot(bone);
        MATRIX_STACK.rotate(bone);
        MATRIX_STACK.scale(bone);
        MATRIX_STACK.moveBackFromPivot(bone);

        if (isBoneRenderOverriden(animatable, bone)) {
            drawOverridenBone(animatable, bone);
            MATRIX_STACK.pop();
            return;
        }

        if (!bone.isHidden()) {
            for (GeoCube cube : bone.childCubes) {
                MATRIX_STACK.push();
                GlStateManager.pushMatrix();
                try {
                    renderCube(builder, cube, red, green, blue, alpha);
                } catch (Exception e) {
                    if (ConfigHandler.debugPrintStacktraces) {
                        e.printStackTrace();
                    }
                } finally {
                    GlStateManager.popMatrix();
                    MATRIX_STACK.pop();
                }
            }
        }
        if (!bone.childBonesAreHiddenToo()) {
            for (GeoBone childBone : bone.childBones) {
                renderRecursively(builder, animatable, childBone, red, green, blue, alpha);
            }
        }

        MATRIX_STACK.pop();
    }

    default void renderCube(Tessellator builder, GeoCube cube, float red, float green, float blue, float alpha) {
        MATRIX_STACK.moveToPivot(cube);
        MATRIX_STACK.rotate(cube);
        MATRIX_STACK.moveBackFromPivot(cube);

        boolean flat = cube.size.x == 0 || cube.size.y == 0 || cube.size.z == 0;
        if (flat) {
            GlStateManager.enablePolygonOffset();
            GlStateManager.doPolygonOffset(-1.0F, -10.0F);
        }

        for (GeoQuad quad : cube.quads) {
            if (quad == null) continue;
            Vector3f normal = new Vector3f(quad.normal.getX(), quad.normal.getY(), quad.normal.getZ());

            MATRIX_STACK.getNormalMatrix().transform(normal);

            /*
             * Fix shading dark shading for flat cubes + compatibility wish Optifine shaders
             */
            if ((cube.size.y == 0 || cube.size.z == 0) && normal.x < 0) {
                normal.x *= -1;
            }
            if ((cube.size.x == 0 || cube.size.z == 0) && normal.y < 0) {
                normal.y *= -1;
            }
            if ((cube.size.x == 0 || cube.size.y == 0) && normal.z < 0) {
                normal.z *= -1;
            }

            for (GeoVertex vertex : quad.vertices) {
                Vector4f vector4f = new Vector4f(vertex.position.x, vertex.position.y, vertex.position.z,
                    1.0F);

                MATRIX_STACK.getModelMatrix().transform(vector4f);
                builder.setColorRGBA_F(red, green, blue, alpha);
                builder.setNormal(normal.x, normal.y, normal.z);
                builder.addVertexWithUV(vector4f.x, vector4f.y, vector4f.z, vertex.textureU, vertex.textureV);
            }
        }

        if (flat) {
            GlStateManager.disablePolygonOffset();
            GlStateManager.doPolygonOffset(0.0F, 0.0F);
        }
    }

    /*
    (-0.4095761, 0.5882118, 0.70710677, 1.0)
    (0.409576, 1.1617882, 0.70710677, 1.0)
    (0.0039961934, 1.7410161, -5.9604645E-8, 1.0)
    (-0.81515586, 1.1674397, -5.9604645E-8, 1.0)
    (-0.003996223, 0.008983791, -5.9604645E-8, 1.0)
    (0.81515586, 0.5825603, -5.9604645E-8, 1.0)
    (0.40957603, 1.1617882, -0.7071068, 1.0)
    (-0.40957603, 0.5882118, -0.7071068, 1.0)
     */
    @SuppressWarnings("rawtypes")
    GeoModelProvider getGeoModelProvider();

    ResourceLocation getTextureLocation(T instance);

    default void renderEarly(GeoModel model, T animatable, float ticks, float red, float green, float blue, float alpha) {
    }

    default void renderLate(GeoModel model, T animatable, float ticks, float red, float green, float blue, float alpha) {
    }

    default void renderAfter(GeoModel model, T animatable, float ticks, float red, float green, float blue, float alpha) {
        drawParticles(model, animatable, ticks);
    }

    default Color getRenderColor(T animatable, float partialTicks) {
        return Color.ofRGBA(255, 255, 255, 255);
    }

    default Integer getUniqueID(T animatable) {
        return animatable.hashCode();
    }

    default void drawParticles(GeoModel model, T animatableArg, float ticks) {
        if (!(animatableArg instanceof IAnimatable)) return;
        IAnimatable animatable = (IAnimatable) animatableArg;
        Map<String, AnimationController> controllerMap = animatable.getFactory().getOrCreateAnimationData(getUniqueID(animatableArg)).getAnimationControllers();
        for (AnimationController controller : controllerMap.values()) {
            for (int i = 0; i < controller.emitters.size(); i++) {
                BedrockEmitter emitter = (BedrockEmitter) controller.emitters.get(i);
                String locator = ((BedrockEmitter) controller.emitters.get(i)).locator + "_locator";//emitter.locator+"_locator";
                if (emitter.locator != null && model.getBone(locator).isPresent()) {
                    GeoBone bone = model.getBone(locator).get();
                    renderParticle(emitter, bone, ticks);
                }
            }
        }
    }

    default void renderParticle(BedrockEmitter emitter, GeoBone locator, float ticks) {
        Matrix4f tempMatrix = new Matrix4f();
        emitter.prevGlobal.x = emitter.lastGlobal.x;
        emitter.prevGlobal.y = emitter.lastGlobal.y;
        emitter.prevGlobal.z = emitter.lastGlobal.z;

        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        Vector3d position = PositionUtils.getCurrentRenderPos();
        //Vector3d position = new Vector3d(0,0,0);
        double posX = position.x;//-376.5;
        double posY = position.y;//8;
        double posZ = position.z;//569.5;

        emitter.lastGlobal.x = posX; //TODO
        emitter.lastGlobal.y = posY; //TODO
        emitter.lastGlobal.z = posZ; //TODO
        RenderHelper.disableStandardItemLighting();

        GL11.glPushMatrix();

        Matrix4f curRot = PositionUtils.getCurrentMatrix();

        PositionUtils.setInitialWorldPos();

        Matrix4f cur2 = PositionUtils.getCurrentRotation(curRot, PositionUtils.getCurrentMatrix());

        emitter.rotation.setIdentity();

        MATRIX_STACK.push();
        tempMatrix.set3x3(cur2);
        MATRIX_STACK.getModelMatrix().mul(tempMatrix);
        GeoBone[] bonePath = getPathFromRoot(locator);
        for (int i = 0; i < bonePath.length; i++) {
            GeoBone bone = bonePath[i];
            MATRIX_STACK.translate(bone);
            MATRIX_STACK.moveToPivot(bone);
            MATRIX_STACK.rotate(bone);
            MATRIX_STACK.scale(bone);
            MATRIX_STACK.moveBackFromPivot(bone);
        }
        MATRIX_STACK.moveToPivot(locator);
        //MATRIX_STACK.translate(6f/16f,16f/16f,0);
        //MATRIX_STACK.rotateX((float) (Math.PI/2));
        //MATRIX_STACK.scale(0.5f,0.5f,0.5f);

        Matrix4f full = MATRIX_STACK.getModelMatrix();
        emitter.rotation = new Matrix3f(full.m00, full.m01, full.m02, full.m10, full.m11, full.m12, full.m20, full.m21, full.m22);
        emitter.lastGlobal.x += full.m03;
        emitter.lastGlobal.y += full.m13;
        emitter.lastGlobal.z += full.m23;

        MATRIX_STACK.pop();
        emitter.render(Minecraft.getMinecraft().timer.renderPartialTicks);
        RenderHelper.enableStandardItemLighting();
        GL11.glPopMatrix();
    }

    default GeoBone[] getPathFromRoot(GeoBone bone) {
        ArrayList<GeoBone> bones = new ArrayList<>();
        while (bone != null) {
            bones.add(0, bone);
            bone = bone.parent;
        }
        return bones.toArray(new GeoBone[0]);
    }
}
