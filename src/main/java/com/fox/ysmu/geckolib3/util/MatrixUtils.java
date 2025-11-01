package com.fox.ysmu.geckolib3.util;

import com.fox.ysmu.mclib.utils.Interpolations;
import com.fox.ysmu.mclib.utils.MathHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.joml.*;

import javax.annotation.Nullable;
//import javax.vecmath.Matrix3f;
//import javax.vecmath.Matrix4d;
//import javax.vecmath.Matrix4f;
//import javax.vecmath.Quat4d;
//import javax.vecmath.SingularMatrixException;
//import javax.vecmath.Vector3d;
//import javax.vecmath.Vector3f;
//import javax.vecmath.Vector4d;
//import javax.vecmath.Vector4f;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

@SideOnly(Side.CLIENT)
public class MatrixUtils {
    /**
     * Model view matrix buffer
     */
    public static final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);

    /**
     * Float array for transferring data from FloatBuffer to the matrix
     */
    public static final float[] floats = new float[16];

    /**
     * Model view matrix captured here
     */
    public static Matrix4f matrix;

    private static final DoubleBuffer doubleBuffer = BufferUtils.createDoubleBuffer(16);
    private static final double[] doubles = new double[16];
    private static final Matrix4d camera = new Matrix4d();

    public static Matrix4d getCameraMatrix() {
        return new Matrix4d(camera);
    }

    /**
     * Read OpenGL's model view matrix
     */
    public static Matrix4f readModelView(Matrix4f dest) {
        buffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
        dest.set(buffer);
        return dest;
    }

    /**
     * This method is called by the ASMAfterCamera method. It saves the camera modelview matrix after the camera is set up.
     * The camera matrix will be used to calculate global transformations of models.
     * Thank you to MiaoNLI for discovering this possibility.
     */
    private static void readCamera() {
        doubleBuffer.clear();
        GL11.glGetDouble(GL11.GL_MODELVIEW_MATRIX, doubleBuffer);
        doubleBuffer.get(doubles);
        camera.set(doubles);
        camera.transpose();
    }

    public static Matrix4f readModelView() {
        return readModelView(new Matrix4f());
    }

    public static Matrix4d readModelViewDouble() {
        doubleBuffer.clear();
        GL11.glGetDouble(GL11.GL_MODELVIEW_MATRIX, doubleBuffer);
        return new Matrix4d().set(doubleBuffer);
    }

    /**
     * Replace model view matrix with given matrix
     */
    public static void loadModelView(Matrix4f matrix4f) {
        matrix4f.get(buffer);
        GL11.glLoadMatrix(buffer);
    }

    public static boolean captureMatrix() {
        if (matrix == null) {
            matrix = MatrixUtils.readModelView(new Matrix4f());

            return true;
        }

        return false;
    }

    public static void ASMAfterCamera() {
        readCamera();
    }

    public static void releaseMatrix() {
        matrix = null;
    }

    public static Quaterniond matrixToQuaternion(Matrix3f matrix) {
        return new Quaterniond().setFromNormalized(matrix);
    }


    // TODO 这里之后都要改
    /**
     * This method calculates the angular velocity around the arbitrary axis.
     * The arbitrary axis and the angle around it can be obtained from this result.
     *
     * @param rotation
     * @return the angular velocity vector.
     */
    public static Vector3f getAngularVelocity(Matrix3f rotation) {
        Matrix3f step = new Matrix3f(rotation);
        Matrix3f angularVelocity = new Matrix3f();
        Matrix3f i = new Matrix3f();

        i.setIdentity();
        angularVelocity.setIdentity();
        angularVelocity.mul(2);

        step.add(i);
        step.invert();
        step.mul(4);

        angularVelocity.sub(step);

        Vector3f angularV = new Vector3f(angularVelocity.m21,
            -angularVelocity.m20,
            angularVelocity.m10);

        return angularV;
    }

    /**
     * Calculate an intrinsic rotation matrix based on the given order and the angles.
     *
     * @param x     angle in radians
     * @param y     angle in radians
     * @param z     angle in radians
     * @param order order of the rotation matrix
     * @return intrinsic rotation matrix
     */
    public static Matrix4f getRotationMatrix(float x, float y, float z, RotationOrder order) {
        Matrix4f mat = new Matrix4f(); // Automatically initialized to identity
        // The original code was equivalent to post-multiplying in the specified order.
        // E.g., for ZYX: mat = I * rotZ * rotY * rotX
        switch (order) {
            case XYZ: return mat.rotateX(x).rotateY(y).rotateZ(z);
            case XZY: return mat.rotateX(x).rotateZ(z).rotateY(y);
            case YXZ: return mat.rotateY(y).rotateX(x).rotateZ(z);
            case YZX: return mat.rotateY(y).rotateZ(z).rotateX(x);
            case ZXY: return mat.rotateZ(z).rotateX(x).rotateY(y);
            case ZYX: return mat.rotateZ(z).rotateY(y).rotateX(x);
            default:  return mat;
        }
    }

    /**
     * Calculates the current global transformations
     *
     * @return Matrix4d array {translation, rotation, scale} or null if singular matrix exception occured during inverting the camera matrix.
     */
    public static Matrix4d[] getTransformation() {
        Matrix4d parent = getCameraMatrix(); // Use the getter to get a copy
        if (!parent.invert()) {
            return null; // Inversion failed
        }

        // Original: parent = parent * readModelViewDouble()
        parent.mul(readModelViewDouble());

        Entity renderViewEntity = Minecraft.getMinecraft().renderViewEntity;
        float partialTicks = Minecraft.getMinecraft().timer.renderPartialTicks;

        // Create camera translation matrix
        Matrix4d cameraTrans = new Matrix4d().translation(
            Interpolations.lerp(renderViewEntity.lastTickPosX, renderViewEntity.posX, partialTicks),
            Interpolations.lerp(renderViewEntity.lastTickPosY, renderViewEntity.posY, partialTicks),
            Interpolations.lerp(renderViewEntity.lastTickPosZ, renderViewEntity.posZ, partialTicks)
        );

        // Original: parent = cameraTrans * parent
        parent.premul(cameraTrans);

        // Decompose using JOML's methods
        Matrix4d translation = new Matrix4d().translation(parent.getTranslation(new Vector3d()));
        Matrix4d rotation = new Matrix4d().set(parent.getNormalizedRotation(new Quaterniond()));
        Matrix4d scale = new Matrix4d().scaling(parent.getScale(new Vector3d()));

        return new Matrix4d[]{translation, rotation, scale};
    }

    /**
     * Extracts transformations from a model-view matrix, optionally removing the camera's influence.
     * Simplified with JOML's decomposition.
     */
    public static Transformation extractTransformations(@Nullable Matrix4f cameraMatrix, Matrix4f modelView) {
        Matrix4f parent = new Matrix4f(modelView);

        if (cameraMatrix != null) {
            // Remove camera transform: modelMatrix = cameraMatrix^-1 * modelViewMatrix
            Matrix4f invCamera = new Matrix4f(cameraMatrix);
            if (!invCamera.invert()) {
                Transformation transformation = new Transformation();
                transformation.creationException = new Exception("Camera matrix is not invertible.");
                return transformation;
            }
            invCamera.mul(modelView, parent);
        }

        // Decompose using JOML's built-in, robust methods
        Vector3f translationVec = parent.getTranslation(new Vector3f());
        Quaternionf rotationQuat = parent.getNormalizedRotation(new Quaternionf());
        Vector3f scaleVec = parent.getScale(new Vector3f());

        Matrix4f translationMat = new Matrix4f().translation(translationVec);
        Matrix4f rotationMat = new Matrix4f().rotation(rotationQuat);
        Matrix4f scaleMat = new Matrix4f().scaling(scaleVec);

        return new Transformation(translationMat, rotationMat, scaleMat);
    }

    // Overloaded method to maintain API compatibility, but MatrixMajor is no longer needed
    // as JOML's decomposition methods are robust.
    public static Transformation extractTransformations(@Nullable Matrix4f cameraMatrix, Matrix4f modelView, MatrixMajor major) {
        // We can ignore 'major' as JOML's math is consistent.
        return extractTransformations(cameraMatrix, modelView);
    }

    public static class Transformation {
        public Matrix4f translation = new Matrix4f();
        public Matrix4f rotation = new Matrix4f();
        public Matrix4f scale = new Matrix4f();
        /**
         * contains the exception that may have
         * occurred during calculation of transformation data
         */
        private Exception creationException = null;

        public Transformation(Matrix4f translation, Matrix4f rotation, Matrix4f scale) {
            this.translation.set(translation);
            this.rotation.set(rotation);
            this.scale.set(scale);
        }

        public Transformation() {
        }

        public Matrix3f getScale3f() {
            return new Matrix3f().scaling(this.scale.getScale(new Vector3f()));
        }

        public Vector3f getTranslation3f() {
            return this.translation.getTranslation(new Vector3f());
        }

        public Matrix3f getRotation3f() {
            return this.rotation.get3x3(new Matrix3f());
        }

        public Exception getCreationException() {
            return this.creationException;
        }

        public Vector3f getRotation(RotationOrder order) {
            return this.getRotation(order, null);
        }

        public Vector3f getRotation(RotationOrder order, Vector3f ref) {
            return this.getRotation(order, ref, 0);
        }

        public Vector3f getRotation(RotationOrder order, int invAxis) {
            return this.getRotation(order, null, invAxis);
        }

        /**
         * Get rotation values from matrix<br>
         * <b>It must be called first to determine if the value can be obtained
         * properly</b>
         *
         * @param order   Rotation Order
         * @param ref     The vector used for reference. Null if nothing to refer. Make
         *                sure that the incoming matrix is not affected by negative
         *                scaling.
         * @param invAxis The axis to be inverted when the matrix is a left-handed
         *                coordinate system. 012 equals xyz
         * @return A vector includes rotation values with specific order, null if matrix
         * is a illegal rotation matrix.<br>
         * The returned vector will be as close to the reference vector as
         * possible
         */
        public Vector3f getRotation(RotationOrder order, Vector3f ref, int invAxis) {
            Matrix3f mat = this.getRotation3f();
            float[] rotation = new float[3];
            float[] refFloats = null;
            if (ref != null) {
                refFloats = new float[3];
                ref.get(refFloats);
            }

            // DirectX -> OpenGL
            // If the scaling value has an odd number of negative values, this will cause it
            // to become a left-handed coordinate system.
            Vector3f x = new Vector3f(mat.m00, mat.m10, mat.m20);
            Vector3f y = new Vector3f(mat.m01, mat.m11, mat.m21);
            Vector3f z = new Vector3f(mat.m02, mat.m12, mat.m22);
            Vector3f crossY = new Vector3f();
            Vector3f originalY = new Vector3f();
            originalY.normalize(y);
            crossY.cross(z, x);
            crossY.normalize();

            if (crossY.dot(originalY) < 0) {
                mat.mul(getInvertAxisMatrix(invAxis));
            }

            Float angle = order.doTest(order.thirdIndex, mat);

            /* if the second rotation value is +/-90, here will be null. */
            if (angle != null) {
                if (refFloats != null) {
                    angle = refFloats[order.thirdIndex] + MathHelper.wrapDegrees(2F * (angle - refFloats[order.thirdIndex])) / 2F;
                }
                rotation[order.thirdIndex] = angle;
                mat.mul(getRotationMatrix(order.thirdIndex, -angle), mat);
            } else if (refFloats != null) {
                rotation[order.thirdIndex] = angle = refFloats[order.thirdIndex];
                mat.mul(getRotationMatrix(order.thirdIndex, -angle), mat);
            }
            angle = order.doTest(order.secondIndex, mat);

            if (angle == null) {
                // Illegal: Scale is zero, no rotation information here.
                return null;
            }

            if (refFloats != null) {
                angle = refFloats[order.secondIndex] + MathHelper.wrapDegrees(angle - refFloats[order.secondIndex]);
            }

            rotation[order.secondIndex] = angle;
            mat.mul(getRotationMatrix(order.secondIndex, -angle), mat);

            angle = order.doTest(order.firstIndex, mat);

            if (angle == null) {
                return null;
            } else if (refFloats != null) {
                angle = refFloats[order.firstIndex] + MathHelper.wrapDegrees(angle - refFloats[order.firstIndex]);
            }

            rotation[order.firstIndex] = angle;

            return new Vector3f(rotation);
        }

        public Vector3f getScale() {
            return this.getScale(0);
        }

        public Vector3f getScale(int invAxis) {
            Vector3f scale = new Vector3f(this.scale.m00, this.scale.m11, this.scale.m22);
            Vector3f x = new Vector3f(this.rotation.m00, this.rotation.m10, this.rotation.m20);
            Vector3f y = new Vector3f(this.rotation.m01, this.rotation.m11, this.rotation.m21);
            Vector3f z = new Vector3f(this.rotation.m02, this.rotation.m12, this.rotation.m22);
            Vector3f crossY = new Vector3f();
            Vector3f originalY = new Vector3f();

            originalY.normalize(y);
            crossY.cross(z, x);
            crossY.normalize();

            if (crossY.dot(originalY) < 0) {
                getInvertAxisMatrix(invAxis).transform(scale);
            }

            return scale;
        }

        public static Matrix3f getRotationMatrix(int axis, double degrees) {
            Matrix3f mat = new Matrix3f();

            switch (axis) {
                case 0:
                    mat.rotX((float) Math.toRadians(degrees));
                    break;
                case 1:
                    mat.rotY((float) Math.toRadians(degrees));
                    break;
                case 2:
                    mat.rotZ((float) Math.toRadians(degrees));
                    break;
            }

            return mat;
        }

        public static Matrix3f getInvertAxisMatrix(int axis) {
            Matrix3f mat = new Matrix3f();

            mat.setIdentity();

            switch (axis) {
                case 0:
                    mat.m00 = -1;
                    break;
                case 1:
                    mat.m11 = -1;
                    break;
                case 2:
                    mat.m22 = -1;
                    break;
            }

            return mat;
        }
    }

    public enum MatrixMajor {
        ROW,
        COLUMN
    }

    public enum RotationOrder {
        XYZ, XZY, YXZ, YZX, ZXY, ZYX;

        public final int firstIndex;
        public final int secondIndex;
        public final int thirdIndex;

        private RotationOrder() {
            String order = this.name().toUpperCase();
            firstIndex = order.charAt(0) - 'X';
            secondIndex = order.charAt(1) - 'X';
            thirdIndex = order.charAt(2) - 'X';
        }

        public Float doTest(int index, Matrix3f test) {
            float[] buffer = new float[3];

            buffer[index == firstIndex ? secondIndex : firstIndex] = 1;

            Vector3f in = new Vector3f(buffer);
            Vector3f out = new Vector3f();

            test.transform(in, out);
            out.get(buffer);
            buffer[index] = 0;
            out.set(buffer);

            if (out.length() < 1E-07) {
                return null;
            }

            out.normalize();

            float cos = in.dot(out);

            out.cross(in, out);
            out.get(buffer);

            float sin = out.length() * Math.signum(buffer[index]);

            return (float) Math.toDegrees(Math.atan2(sin, cos));
        }
    }
}
