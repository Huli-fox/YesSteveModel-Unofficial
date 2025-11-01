package com.fox.ysmu.geckolib3.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import org.joml.Matrix3f;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class PositionUtils {
    public static void setInitialWorldPos() {
        Entity camera = Minecraft.getMinecraft().renderViewEntity;
        GL11.glLoadIdentity();
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
            GL11.glScaled(-1, 1, -1);
        } else if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
            GL11.glScaled(-1, 1, -1);
        } else {
            GL11.glScaled(1, 1, 1);
        }
        GL11.glRotatef(-camera.rotationPitch, 1, 0, 0);
        GL11.glRotatef(camera.rotationYaw, 0, 1, 0);
        GL11.glTranslated(-RenderManager.renderPosX, -RenderManager.renderPosY, -RenderManager.renderPosZ);
        Vector3d additional = getCameraShift();
        GL11.glTranslated(additional.x, additional.y, additional.z);
    }

    public static Vector3d getCameraShift() {
        Vector3d res = new Vector3d(0, 0, 0);
        int thirdPersonView = Minecraft.getMinecraft().gameSettings.thirdPersonView;

        if (thirdPersonView == 0) {
            return res;
        } else {
            Vec3 look = Minecraft.getMinecraft().thePlayer.getLookVec();
            res.set(look.xCoord, look.yCoord, look.zCoord);

            if (thirdPersonView == 1) {
                // vecmath 的 scale(s) 对应 joml 的 mul(s)
                res.mul(4);
            } else { // thirdPersonView == 2
                res.mul(-4);
            }
            return res;
        }
    }

    public static Vector3d getCurrentRenderPos() {
        Entity camera = Minecraft.getMinecraft().renderViewEntity;
        Matrix4f matrix4f = getCurrentMatrix();
        MatrixUtils.Transformation transformation = MatrixUtils.extractTransformations(null, matrix4f);
        double dl = matrix4f.m30(); // 注意：JOML是列主序，平移分量在 m30, m31, m32
        double du = matrix4f.m31(); // 而 vecmath 是行主序，在 m03, m13, m23。这是一个关键区别！
        double dz = matrix4f.m32();
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
            dz += 4;
        }
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2) {
            dz *= -1;
            dl *= -1;
            dz -= 4;
        }
        Matrix4d rotMatrixX = new Matrix4d().rotationX(Math.toRadians(camera.rotationPitch));
        Matrix4d rotMatrixY = new Matrix4d().rotationY(Math.toRadians(-camera.rotationYaw));
        Vector3d vecZ = new Vector3d(0, 0, 1);
        rotMatrixX.transformDirection(vecZ);
        rotMatrixY.transformDirection(vecZ);
        vecZ.mul(-1);

        Vector3d vecL = new Vector3d(1, 0, 0);
        rotMatrixX.transformDirection(vecL);
        rotMatrixY.transformDirection(vecL);
        vecL.mul(-1);

        Vector3d vecU = new Vector3d(0, 1, 0);
        rotMatrixX.transformDirection(vecU);
        rotMatrixY.transformDirection(vecU);

        vecZ.mul(dz);
        vecU.mul(du);
        vecL.mul(dl);
        Vector3d pos = new Vector3d(vecZ).add(vecU).add(vecL);
        Vector3d res = pos.add(RenderManager.renderPosX, RenderManager.renderPosY, RenderManager.renderPosZ);
        return res;
    }

    public static Matrix4f getCurrentMatrix() {
        MatrixUtils.matrix = null;
        MatrixUtils.captureMatrix();
        return MatrixUtils.matrix;
    }

    public static Matrix4f getBasicRotation() {
        Entity camera = Minecraft.getMinecraft().renderViewEntity;
        // 使用JOML的流式API构建旋转矩阵
        // 这里使用rotate，因为它等效于原代码的 mul 操作
        // 原代码的 camera.rotationPitch / 360 * Math.PI / 2 简化为 Math.toRadians(camera.rotationPitch / 2.0)
        return new Matrix4f()
            .identity()
            .rotateX((float) Math.toRadians(camera.rotationPitch / 2.0)) // vecmath的rotX是设置，joml的rotationX是设置，rotate是累乘
            .rotateY((float) Math.toRadians(camera.rotationYaw / 2.0));
    }

    public static void changeToRot(Matrix4f current, Matrix4f rot) {
        current.set3x3(rot);
    }

    public static Matrix3f getRotBlock(Matrix4f rot) {
        return new Matrix3f(rot);
    }

    public static Matrix4f getCurrentRotation(Matrix4f old, Matrix4f base) {
        base.invert();
        Matrix4f rot = new Matrix4f().set3x3(old);
        base.mul(rot);

        return base;
    }
}
