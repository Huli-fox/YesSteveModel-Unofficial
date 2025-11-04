package com.fox.ysmu.compat;

import org.joml.Quaternionf;
import org.lwjgl.util.vector.Quaternion;

public class QuatJ2L {
    public static Quaternion j2l(Quaternionf jomlQuat) {
        Quaternion lwjglQuat = new Quaternion();
        lwjglQuat.set(jomlQuat.x, jomlQuat.y, jomlQuat.z, jomlQuat.w);
        return lwjglQuat;
    }
}
