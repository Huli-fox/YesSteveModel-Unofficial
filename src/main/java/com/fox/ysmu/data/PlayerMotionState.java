package com.fox.ysmu.data;

import net.minecraft.entity.player.EntityPlayer;

public final class PlayerMotionState {

    public static final int ON_GROUND = 0x01;
    public static final int IS_FLYING = 0x02;

    private PlayerMotionState() {}

    public static byte pack(EntityPlayer player) {
        byte flags = 0;
        flags = setFlag(flags, ON_GROUND, player.onGround);
        flags = setFlag(flags, IS_FLYING, player.capabilities.isFlying);
        return flags;
    }

    public static boolean isOnGround(byte flags) {
        return (flags & ON_GROUND) != 0;
    }

    public static boolean isFlying(byte flags) {
        return (flags & IS_FLYING) != 0;
    }

    private static byte setFlag(byte flags, int flag, boolean enabled) {
        if (enabled) {
            return (byte) (flags | flag);
        }
        return (byte) (flags & ~flag);
    }
}
