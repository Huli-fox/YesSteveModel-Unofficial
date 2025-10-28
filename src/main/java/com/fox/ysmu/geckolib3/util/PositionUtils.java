package com.fox.ysmu.geckolib3.util;

import com.eliotlash.mclib.utils.MatrixUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4d;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3d;

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
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
            return res;
        } else if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
            Vec3 look = Minecraft.getMinecraft().thePlayer.getLookVec();
            res = new Vector3d(look.xCoord, look.yCoord, look.zCoord);
            res.scale(4);
            return res;
        } else {
            Vec3 look = Minecraft.getMinecraft().thePlayer.getLookVec();
            res = new Vector3d(look.xCoord, look.yCoord, look.zCoord);
            res.scale(-4);
            return res;
        }
    }

    public static Vector3d getCurrentRenderPos() {
        Entity camera = Minecraft.getMinecraft().renderViewEntity;
        Matrix4f matrix4f = getCurrentMatrix();
        MatrixUtils.Transformation transformation = MatrixUtils.extractTransformations(null, matrix4f);
        double dl = matrix4f.m03;
        double du = matrix4f.m13;
        double dz = matrix4f.m23;
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
            dz += 4;
        }
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2) {
            dz *= -1;
            dl *= -1;
            dz -= 4;
        }
        Matrix4d rotMatrixX = new Matrix4d();
        rotMatrixX.rotX((camera.rotationPitch) / 360 * Math.PI * 2);
        Matrix4d rotMatrixY = new Matrix4d();
        rotMatrixY.rotY((-camera.rotationYaw) / 360 * Math.PI * 2);
        Vector3d vecZ = new Vector3d(0, 0, 1);
        rotMatrixX.transform(vecZ);
        rotMatrixY.transform(vecZ);
        vecZ.scale(-1);

        Vector3d vecL = new Vector3d(1, 0, 0);
        rotMatrixX.transform(vecL);
        rotMatrixY.transform(vecL);
        vecL.scale(-1);

        Vector3d vecU = new Vector3d(0, 1, 0);
        rotMatrixX.transform(vecU);
        rotMatrixY.transform(vecU);

        vecZ.scale(dz);
        vecU.scale(du);
        vecL.scale(dl);
        Vector3d pos = new Vector3d(vecZ.x + vecU.x + vecL.x, vecZ.y + vecU.y + vecL.y, vecZ.z + vecU.z + vecL.z);
        Vector3d res = new Vector3d(pos.x + RenderManager.renderPosX, pos.y + RenderManager.renderPosY, pos.z + RenderManager.renderPosZ);
        return res;
    }

    public static Matrix4f getCurrentMatrix() {
        MatrixUtils.matrix = null;
        MatrixUtils.captureMatrix();
        return MatrixUtils.matrix;
    }

    public static Matrix4f getBasicRotation() {
        Entity camera = Minecraft.getMinecraft().renderViewEntity;
        Matrix4f basicRot = new Matrix4f();
        basicRot.rotX((float) (camera.rotationPitch / 360 * Math.PI / 2));
        Matrix4f yRot = new Matrix4f();
        yRot.rotY((float) (camera.rotationYaw / 360 * Math.PI / 2));
        basicRot.mul(yRot);
        return basicRot;
    }

    public static void changeToRot(Matrix4f current, Matrix4f rot) {
        current.m00 = rot.m00;
        current.m01 = rot.m01;
        current.m02 = rot.m02;
        current.m10 = rot.m10;
        current.m11 = rot.m11;
        current.m12 = rot.m12;
        current.m20 = rot.m20;
        current.m21 = rot.m21;
        current.m22 = rot.m22;
    }

    public static Matrix3f getRotBlock(Matrix4f rot) {
        return new Matrix3f(rot.m00, rot.m01, rot.m02, rot.m10, rot.m11, rot.m12, rot.m20, rot.m21, rot.m22);
    }

    public static Matrix4f getCurrentRotation(Matrix4f old, Matrix4f base) {
        base.invert();
        Matrix4f rot = new Matrix4f(old.m00, old.m01, old.m02, 0, old.m10, old.m11, old.m12, 0, old.m20, old.m21, old.m22, 0, 0, 0, 0, old.m33);
        base.mul(rot);

        return base;
    }
}
