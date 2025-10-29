package com.fox.ysmu.geckolib3.geo;

// [1.7.10] MODIFIED: 导入适配1.7.10的类
import com.fox.ysmu.geckolib3.core.IAnimatable;
import com.fox.ysmu.geckolib3.core.controller.AnimationController;
import com.fox.ysmu.geckolib3.core.util.Color;
import com.fox.ysmu.geckolib3.geo.render.built.*;
import com.fox.ysmu.geckolib3.model.provider.GeoModelProvider;
import com.fox.ysmu.geckolib3.util.EModelRenderCycle;
import com.fox.ysmu.geckolib3.util.IRenderCycle;
import com.fox.ysmu.geckolib3.util.MatrixStack;
import com.fox.ysmu.util.Keep;
import com.fox.ysmu.geckolib3.util.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.Map;

// [1.7.10] NOTE: 以下高版本导入已被移除或替换:
// import com.mojang.blaze3d.vertex.PoseStack;
// import com.mojang.blaze3d.vertex.VertexConsumer;
// import net.minecraft.client.renderer.LightTexture; // Replaced with OpenGlHelper
// import net.minecraft.client.renderer.MultiBufferSource;
// import net.minecraft.client.renderer.RenderType;
// import org.joml.*; // Replaced with javax.vecmath

@SuppressWarnings({"rawtypes", "unchecked"})
public interface IGeoRenderer<T> {
    // [1.7.10] ADDED: 1.7.10移植版使用自定义的MatrixStack进行变换，替代PoseStack。
    MatrixStack MATRIX_STACK = new MatrixStack();
    // [1.7.10] KEPT: 从高版本保留，用于辉光效果的逻辑。
    String GLOW_PREFIX = "ysmGlow";

    @Keep
    GeoModelProvider getGeoModelProvider();

    @Keep
    ResourceLocation getTextureLocation(T animatable);

    @Keep
    @Nullable
    default GeoModel getGeoModel() {
        return null;
    }

    // [1.7.10] MODIFIED: 主渲染方法。保留了高版本的签名以实现兼容性，但实现已完全替换为1.7.10的Tessellator逻辑。
    // 高版本参数如PoseStack, RenderType, VertexConsumer等将被忽略。
    @Keep
    default void render(GeoModel model, T animatable, float partialTick, float red, float green, float blue, float alpha) {

        // 1.7.10 GL状态设置
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        GlStateManager.enableRescaleNormal();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // 调用生命周期方法
        renderEarly(animatable, partialTick, red, green, blue, alpha);

        renderLate(animatable, partialTick, red, green, blue, alpha);

        // 使用Tessellator进行绘制
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_QUADS);

        // 渲染所有根骨骼
        for (GeoBone group : model.topLevelBones) {
            renderRecursively(group, red, green, blue, alpha);
        }

        tessellator.draw();

        // 恢复GL状态
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.popMatrix();

        // 设置渲染循环状态
        setCurrentModelRenderCycle(EModelRenderCycle.REPEATED);
    }

    // [1.7.10] MODIFIED: 骨骼递归渲染。签名保留，实现替换为1.7.10的MatrixStack逻辑。
    // 使用packedLight参数来模拟辉光效果。
    @Keep
    default void renderRecursively(GeoBone bone, float red, float green, float blue, float alpha) {
        boolean isGlowBone = bone.getName().startsWith(GLOW_PREFIX);
        float lastLightmapX = 0, lastLightmapY = 0;

        // 处理辉光效果
        if (isGlowBone) {
            lastLightmapX = OpenGlHelper.lastBrightnessX;
            lastLightmapY = OpenGlHelper.lastBrightnessY;
            // 设置光照贴图坐标为最大亮度
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        }

        MATRIX_STACK.push();
        // 使用自定义MatrixStack应用骨骼变换
        MATRIX_STACK.translate(bone);
        MATRIX_STACK.moveToPivot(bone);
        MATRIX_STACK.rotate(bone);
        MATRIX_STACK.scale(bone);
        MATRIX_STACK.moveBackFromPivot(bone);

        renderCubesOfBone(bone, red, green, blue, alpha);
        renderChildBones(bone, red, green, blue, alpha);

        MATRIX_STACK.pop();

        // 恢复之前的光照
        if (isGlowBone) {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastLightmapX, lastLightmapY);
        }
    }

    // [1.7.10] MODIFIED: 保留高版本结构，实现替换为1.7.10逻辑。
    @Keep
    default void renderCubesOfBone(GeoBone bone, float red, float green, float blue, float alpha) {
        if (bone.isHidden()) {
            return;
        }
        for (GeoCube cube : bone.childCubes) {
            if (!bone.cubesAreHidden()) {
                MATRIX_STACK.push();
                renderCube(cube, red, green, blue, alpha);
                MATRIX_STACK.pop();
            }
        }
    }

    // [1.7.10] MODIFIED: 保留高版本结构，实现替换为1.7.10逻辑。
    @Keep
    default void renderChildBones(GeoBone bone, float red, float green, float blue, float alpha) {
        if (bone.childBonesAreHiddenToo()) {
            return;
        }
        for (GeoBone childBone : bone.childBones) {
            renderRecursively(childBone, red, green, blue, alpha);
        }
    }

    // [1.7.10] MODIFIED: Cube渲染。实现替换为1.7.10的MatrixStack和Tessellator逻辑。
    // createVerticesOfQuad方法的逻辑被内联到此方法中。
    @Keep
    default void renderCube(GeoCube cube, float red, float green, float blue, float alpha) {
        MATRIX_STACK.moveToPivot(cube);
        MATRIX_STACK.rotate(cube);
        MATRIX_STACK.moveBackFromPivot(cube);

        Matrix4f modelMatrix = MATRIX_STACK.getModelMatrix();
        Matrix3f normalMatrix = MATRIX_STACK.getNormalMatrix();
        Tessellator tessellator = Tessellator.instance;

        boolean isFlat = cube.size.x == 0 || cube.size.y == 0 || cube.size.z == 0;
        if (isFlat) {
            GlStateManager.enablePolygonOffset();
            GlStateManager.doPolygonOffset(-1.0F, -10.0F);
        }

        for (GeoQuad quad : cube.quads) {
            if (quad == null) {
                continue;
            }
            Vector3f normal = new Vector3f(quad.normal.x, quad.normal.y, quad.normal.z);
            normalMatrix.transform(normal);

            // 修正平面方块的法线方向
            if ((cube.size.y == 0 || cube.size.z == 0) && normal.x < 0) normal.x *= -1;
            if ((cube.size.x == 0 || cube.size.z == 0) && normal.y < 0) normal.y *= -1;
            if ((cube.size.x == 0 || cube.size.y == 0) && normal.z < 0) normal.z *= -1;

            tessellator.setColorRGBA_F(red, green, blue, alpha);
            tessellator.setNormal(normal.x, normal.y, normal.z);

            for (GeoVertex vertex : quad.vertices) {
                Vector4f pos = new Vector4f(vertex.position.x, vertex.position.y, vertex.position.z, 1.0F);
                modelMatrix.transform(pos);
                // 1.7.10的Tessellator不支持直接传入光照和覆盖层参数，这些由全局GL状态控制
                tessellator.addVertexWithUV(pos.x, pos.y, pos.z, vertex.textureU, vertex.textureV);
            }
        }

        if (isFlat) {
            GlStateManager.disablePolygonOffset();
        }
    }

    // [1.7.10] MODIFIED: 早期渲染回调。使用GlStateManager替代PoseStack进行缩放。
    @Keep
    default void renderEarly(T animatable, float partialTick, float red, float green, float blue, float alpha) {
        if (getCurrentModelRenderCycle() == EModelRenderCycle.INITIAL) {
            float width = getWidthScale(animatable);
            float height = getHeightScale(animatable);
            GlStateManager.scale(width, height, width);
        }
    }

    // [1.7.10] KEPT: 晚期渲染回调，两个版本中均为空实现。
    @Keep
    default void renderLate(T animatable, float partialTick, float red, float green, float blue, float alpha) {
    }

    // [1.7.10] MODIFIED: 签名保留，实现简化，因为多数参数在1.7.10中无用。
    @Keep
    default Color getRenderColor(T animatable, float partialTick) {
        return Color.WHITE;
    }

    // [1.7.10] KEPT: 实例ID获取，两个版本逻辑一致。
    @Keep
    default int getInstanceId(T animatable) {
        return animatable.hashCode();
    }

    // [1.7.10] KEPT: 渲染循环状态控制，对缩放逻辑很重要。
    @Nonnull
    @Keep
    default IRenderCycle getCurrentModelRenderCycle() {
        return EModelRenderCycle.INITIAL;
    }

    // [1.7.10] KEPT: 渲染循环状态控制。
    @Keep
    default void setCurrentModelRenderCycle(IRenderCycle cycle) {
    }

    // [1.7.10] KEPT: 用于renderEarly中的模型缩放。
    @Keep
    default float getWidthScale(T animatable) {
        return 1F;
    }

    // [1.7.10] KEPT: 用于renderEarly中的模型缩放。
    @Keep
    default float getHeightScale(T entity) {
        return 1F;
    }
}
