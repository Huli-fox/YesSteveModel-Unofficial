package com.fox.ysmu.geckolib3.util;

import com.google.common.collect.Queues;
import com.fox.ysmu.geckolib3.geo.render.built.GeoBone;
import com.fox.ysmu.geckolib3.geo.render.built.GeoCube;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Deque;

public class MatrixStack {

    private final Deque<Entry> matrixStack;

    public MatrixStack() {
        this.matrixStack = Queues.newArrayDeque();
        this.matrixStack.add(new Entry(new Matrix4f().identity(), new Matrix3f().identity()));
    }

    /**
     * Pushes a copy of the current matrix onto the stack.
     */
    public void push() {
        final Entry last = this.matrixStack.getLast();
        this.matrixStack.addLast(new Entry(new Matrix4f(last.model), new Matrix3f(last.normal)));
    }

    /**
     * Pops the current matrix from the stack, restoring the previous one.
     */
    public void pop() {
        if (this.matrixStack.size() <= 1) {
            throw new IllegalStateException("Cannot pop the root matrix stack entry!");
        }
        this.matrixStack.removeLast();
    }

    /**
     * Returns the current matrix entry (model and normal) from the top of the stack without removing it.
     */
    public Entry peek() {
        return this.matrixStack.getLast();
    }

    /**
     * Gets the current model matrix from the top of the stack.
     * @return The current 4x4 model matrix.
     */
    public Matrix4f getModelMatrix() {
        return this.peek().getModel();
    }

    /**
     * Gets the current normal matrix from the top of the stack.
     * @return The current 3x3 normal matrix.
     */
    public Matrix3f getNormalMatrix() {
        return this.peek().getNormal();
    }

    /**
     * Resets the current matrices to identity matrices.
     */
    public void loadIdentity() {
        Entry entry = this.peek();
        entry.model.identity();
        entry.normal.identity();
    }

    /**
     * Checks if the stack is at its base level (only the root entry remains).
     */
    public boolean clear() {
        return this.matrixStack.size() == 1;
    }

    /* ====================================================================== */
    /*                            Translate Methods                           */
    /* ====================================================================== */

    public void translate(double x, double y, double z) {
        this.peek().getModel().translate((float)x, (float)y, (float)z);
    }

    public void translate(float x, float y, float z) {
        this.peek().getModel().translate(x, y, z);
    }

    public void translate(Vector3f vec) {
        this.peek().getModel().translate(vec);
    }

    public void moveToPivot(GeoCube cube) {
        Vector3f pivot = cube.pivot;
        this.translate(pivot.x / 16.0f, pivot.y / 16.0f, pivot.z / 16.0f);
    }

    public void moveBackFromPivot(GeoCube cube) {
        Vector3f pivot = cube.pivot;
        this.translate(-pivot.x / 16.0f, -pivot.y / 16.0f, -pivot.z / 16.0f);
    }

    public void moveToPivot(GeoBone bone) {
        this.translate(bone.rotationPointX / 16.0f, bone.rotationPointY / 16.0f, bone.rotationPointZ / 16.0f);
    }

    public void moveBackFromPivot(GeoBone bone) {
        this.translate(-bone.rotationPointX / 16.0f, -bone.rotationPointY / 16.0f, -bone.rotationPointZ / 16.0f);
    }

    public void translate(GeoBone bone) {
        this.translate(-bone.getPositionX() / 16.0f, bone.getPositionY() / 16.0f, bone.getPositionZ() / 16.0f);
    }

    /* ====================================================================== */
    /*                              Scale Methods                             */
    /* ====================================================================== */

    public void scale(float x, float y, float z) {
        Entry entry = this.peek();
        entry.getModel().scale(x, y, z);


        if (x == y && y == z) {
            if (x > 0.0f) {
                return;
            }
            entry.getNormal().scale(-1.0f);
        } else {
            float invX = 1.0f / x;
            float invY = 1.0f / y;
            float invZ = 1.0f / z;
            float i = invSqrt(invX * invY * invZ);
            entry.getNormal().scale(i*invX, i*invY, i*invZ);
        }
    }

    public void scale(GeoBone bone) {
        this.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
    }

    private static float invSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
        x *= (1.5f - xhalf * x * x);
        return x;
    }

    /* ====================================================================== */
    /*                              Rotate Methods                            */
    /* ====================================================================== */

    public void rotateX(float rad) {
        Entry entry = this.peek();
        entry.getModel().rotateX(rad);
        entry.getNormal().rotateX(rad);
    }

    public void rotateY(float rad) {
        Entry entry = this.peek();
        entry.getModel().rotateY(rad);
        entry.getNormal().rotateY(rad);
    }

    public void rotateZ(float rad) {
        Entry entry = this.peek();
        entry.getModel().rotateZ(rad);
        entry.getNormal().rotateZ(rad);
    }

    public void multiply(Quaternionf quaternion) {
        Entry entry = this.peek();
        entry.model.rotate(quaternion);
        entry.normal.rotate(quaternion);
    }

    /**
     * Rotates around a specific origin point.
     */
    public void multiply(Quaternionf quaternion, float originX, float originY, float originZ) {
        Entry entry = this.peek();
        entry.model.rotateAround(quaternion, originX, originY, originZ);
        entry.normal.rotate(quaternion);
    }

    public void rotate(GeoBone bone) {
        // Apply rotations in Z, Y, X order, which is a common convention
        if (bone.getRotationZ() != 0.0f) {
            this.rotateZ(bone.getRotationZ());
        }
        if (bone.getRotationY() != 0.0f) {
            this.rotateY(bone.getRotationY());
        }
        if (bone.getRotationX() != 0.0f) {
            this.rotateX(bone.getRotationX());
        }
    }

    public void rotate(GeoCube cube) {
        Vector3f rotation = cube.rotation;
        Entry entry = this.peek();
        if (rotation.z != 0.0f) {
            entry.getModel().rotateZ(rotation.z);
            entry.getNormal().rotateZ(rotation.z);
        }
        if (rotation.y != 0.0f) {
            entry.getModel().rotateY(rotation.y);
            entry.getNormal().rotateY(rotation.y);
        }
        if (rotation.x != 0.0f) {
            entry.getModel().rotateX(rotation.x);
            entry.getNormal().rotateX(rotation.x);
        }
    }

    /* ====================================================================== */
    /*                              Other Methods                             */
    /* ====================================================================== */

    public void multiplyPositionMatrix(Matrix4f matrix) {
        this.peek().getModel().mul(matrix);
    }


    /**
     * Inner class to hold both model and normal matrices together in the stack.
     */
    public static final class Entry {
        private final Matrix4f model;
        private final Matrix3f normal;

        private Entry(Matrix4f model, Matrix3f normal) {
            this.model = model;
            this.normal = normal;
        }

        public Matrix4f getModel() {
            return model;
        }

        public Matrix3f getNormal() {
            return normal;
        }
    }
}
