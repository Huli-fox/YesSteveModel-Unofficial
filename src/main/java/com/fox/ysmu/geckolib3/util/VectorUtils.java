package com.fox.ysmu.geckolib3.util;

import org.joml.Vector3f;
import net.minecraft.util.Vec3;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

public class VectorUtils {
    public static Vec3 fromArray(double[] array) {
        Validate.validIndex(ArrayUtils.toObject(array), 2);
        return Vec3.createVectorHelper(array[0], array[1], array[2]);
    }

    public static Vector3f fromArray(float[] array) {
        Validate.validIndex(ArrayUtils.toObject(array), 2);
        return new Vector3f(array[0], array[1], array[2]);
    }

    public static Vector3f convertDoubleToFloat(Vec3 vector) {
        return new Vector3f((float) vector.xCoord, (float) vector.yCoord, (float) vector.zCoord);
    }

    public static Vec3 convertFloatToDouble(Vector3f vector) {
        return Vec3.createVectorHelper(vector.x(), vector.y(), vector.z());
    }
}
