package com.fox.ysmu.compat;

import org.joml.Quaternionf;
import org.lwjgl.util.vector.Quaternion;

public class Utils {

    public static Quaternion j2l(Quaternionf jomlQuat) {
        Quaternion lwjglQuat = new Quaternion();
        lwjglQuat.set(jomlQuat.x, jomlQuat.y, jomlQuat.z, jomlQuat.w);
        return lwjglQuat;
    }

    public static boolean isValidResourceLocation(String locationString) {
        if (locationString == null || locationString.isEmpty()) {
            return false;
        }
        try {
            new net.minecraft.util.ResourceLocation(locationString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
