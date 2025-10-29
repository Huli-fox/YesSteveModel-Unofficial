package com.fox.ysmu.geckolib3.util;

import com.fox.ysmu.geckolib3.geo.render.built.GeoBone;
import com.fox.ysmu.geckolib3.geo.render.built.GeoCube;
import com.fox.ysmu.geckolib3.util.GlStateManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class RenderUtils {
    /**
     * 修改：不再使用 PoseStack，而是直接调用 GlStateManager.translate。
     * 将骨骼的局部坐标转换为渲染坐标。
     */
    public static void translateMatrixToBone(GeoBone bone) {
        GlStateManager.translate(-bone.getPositionX() / 16f, bone.getPositionY() / 16f, bone.getPositionZ() / 16f);
    }

    /**
     * 修改：不再使用 PoseStack，而是直接调用 GlStateManager.rotate。
     * 注意：GL的旋转方法使用度（degrees），而GeckoLib内部使用弧度（radians），所以需要转换。
     * 旋转顺序：Z, Y, X，与高版本保持一致。
     */
    public static void rotateMatrixAroundBone(GeoBone bone) {
        if (bone.getRotationZ() != 0.0F) {
            GlStateManager.rotate((float) Math.toDegrees(bone.getRotationZ()), 0, 0, 1);
        }
        if (bone.getRotationY() != 0.0F) {
            GlStateManager.rotate((float) Math.toDegrees(bone.getRotationY()), 0, 1, 0);
        }
        if (bone.getRotationX() != 0.0F) {
            GlStateManager.rotate((float) Math.toDegrees(bone.getRotationX()), 1, 0, 0);
        }
    }

    /**
     * 修改：不再使用 PoseStack，而是直接调用 GlStateManager.rotate。
     * 同样需要将弧度转换为度。
     */
    public static void rotateMatrixAroundCube(GeoCube cube) {
        Vector3f rotation = cube.rotation;
        // 旋转顺序 Z -> Y -> X
        if (rotation.z() != 0.0F) {
            GlStateManager.rotate((float) Math.toDegrees(rotation.z()), 0, 0, 1);
        }
        if (rotation.y() != 0.0F) {
            GlStateManager.rotate((float) Math.toDegrees(rotation.y()), 0, 1, 0);
        }
        if (rotation.x() != 0.0F) {
            GlStateManager.rotate((float) Math.toDegrees(rotation.x()), 1, 0, 0);
        }
    }

    /**
     * 修改：不再使用 PoseStack，而是直接调用 GlStateManager.scale。
     */
    public static void scaleMatrixForBone(GeoBone bone) {
        GlStateManager.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
    }

    /**
     * 修改：不再使用 PoseStack，而是直接调用 GlStateManager.translate。
     * 移动到旋转轴心点。
     */
    public static void translateToPivotPoint(GeoCube cube) {
        Vector3f pivot = cube.pivot;
        GlStateManager.translate(pivot.x() / 16f, pivot.y() / 16f, pivot.z() / 16f);
    }

    /**
     * 修改：不再使用 PoseStack，而是直接调用 GlStateManager.translate。
     */
    public static void translateToPivotPoint(GeoBone bone) {
        GlStateManager.translate(bone.rotationPointX / 16f, bone.rotationPointY / 16f, bone.rotationPointZ / 16f);
    }

    /**
     * 修改：不再使用 PoseStack，而是直接调用 GlStateManager.translate。
     * 从旋转轴心点移回。
     */
    public static void translateAwayFromPivotPoint(GeoCube cube) {
        Vector3f pivot = cube.pivot;
        GlStateManager.translate(-pivot.x() / 16f, -pivot.y() / 16f, -pivot.z() / 16f);
    }

    /**
     * 修改：不再使用 PoseStack，而是直接调用 GlStateManager.translate。
     */
    public static void translateAwayFromPivotPoint(GeoBone bone) {
        GlStateManager.translate(-bone.rotationPointX / 16f, -bone.rotationPointY / 16f, -bone.rotationPointZ / 16f);
    }

    /**
     * 修改：不再使用 PoseStack，直接调用已修改的本地方法。
     */
    public static void translateAndRotateMatrixForBone(GeoBone bone) {
        translateToPivotPoint(bone);
        rotateMatrixAroundBone(bone);
    }

    /**
     * 修改：不再使用 PoseStack，直接调用已修改的本地方法。
     * 这是一个组合方法，用于对一个骨骼应用所有变换。
     */
    public static void prepMatrixForBone(GeoBone bone) {
        translateMatrixToBone(bone);
        translateToPivotPoint(bone);
        rotateMatrixAroundBone(bone);
        scaleMatrixForBone(bone);
        translateAwayFromPivotPoint(bone);
    }

    /**
     * 无需修改：此方法使用 JOML 进行纯数学计算，与Minecraft渲染API无关。
     * 只要您的环境中存在JOML，它就可以正常工作。
     */
    public static Matrix4f invertAndMultiplyMatrices(Matrix4f baseMatrix, Matrix4f inputMatrix) {
        inputMatrix = new Matrix4f(inputMatrix);
        inputMatrix.invert();
        inputMatrix.mul(baseMatrix);
        return inputMatrix;
    }
}
