package com.fox.ysmu.geckolib3.util;

import com.fox.ysmu.geckolib3.geo.render.built.GeoBone;
import com.fox.ysmu.geckolib3.geo.render.built.GeoCube;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RenderUtils {
    public static void translateMatrixToBone(MatrixStack matrixStack, GeoBone bone) {
        matrixStack.translate(-bone.getPositionX() / 16f, bone.getPositionY() / 16f, bone.getPositionZ() / 16f);
    }

    public static void rotateMatrixAroundBone(MatrixStack matrixStack, GeoBone bone) {
        if (bone.getRotationZ() != 0.0F) {
            matrixStack.multiply(new Quaternionf().rotationZ(bone.getRotationZ()));
        }
        if (bone.getRotationY() != 0.0F) {
            matrixStack.multiply(new Quaternionf().rotationY(bone.getRotationY()));
        }
        if (bone.getRotationX() != 0.0F) {
            matrixStack.multiply(new Quaternionf().rotationX(bone.getRotationX()));
        }
    }

    public static void rotateMatrixAroundCube(MatrixStack matrixStack, GeoCube cube) {
        Vector3f rotation = cube.rotation;
        matrixStack.multiply(new Quaternionf().rotationXYZ(0, 0, rotation.z()));
        matrixStack.multiply(new Quaternionf().rotationXYZ(0, (float) rotation.y(), 0));
        matrixStack.multiply(new Quaternionf().rotationXYZ(rotation.x(), 0, 0));
    }

    public static void scaleMatrixForBone(MatrixStack matrixStack, GeoBone bone) {
        matrixStack.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
    }

    public static void translateToPivotPoint(MatrixStack matrixStack, GeoCube cube) {
        Vector3f pivot = cube.pivot;
        matrixStack.translate(pivot.x() / 16f, pivot.y() / 16f, pivot.z() / 16f);
    }

    public static void translateToPivotPoint(MatrixStack matrixStack, GeoBone bone) {
        matrixStack.translate(bone.rotationPointX / 16f, bone.rotationPointY / 16f, bone.rotationPointZ / 16f);
    }

    public static void translateAwayFromPivotPoint(MatrixStack matrixStack, GeoCube cube) {
        Vector3f pivot = cube.pivot;
        matrixStack.translate(-pivot.x() / 16f, -pivot.y() / 16f, -pivot.z() / 16f);
    }

    public static void translateAwayFromPivotPoint(MatrixStack matrixStack, GeoBone bone) {
        matrixStack.translate(-bone.rotationPointX / 16f, -bone.rotationPointY / 16f, -bone.rotationPointZ / 16f);
    }

    public static void translateAndRotateMatrixForBone(MatrixStack matrixStack, GeoBone bone) {
        translateToPivotPoint(matrixStack, bone);
        rotateMatrixAroundBone(matrixStack, bone);
    }

    public static void prepMatrixForBone(MatrixStack matrixStack, GeoBone bone) {
        translateMatrixToBone(matrixStack, bone);
        translateToPivotPoint(matrixStack, bone);
        rotateMatrixAroundBone(matrixStack, bone);
        scaleMatrixForBone(matrixStack, bone);
        translateAwayFromPivotPoint(matrixStack, bone);
    }

    public static Matrix4f invertAndMultiplyMatrices(Matrix4f baseMatrix, Matrix4f inputMatrix) {
        inputMatrix = new Matrix4f(inputMatrix);
        inputMatrix.invert();
        inputMatrix.mul(baseMatrix);
        return inputMatrix;
    }
}
