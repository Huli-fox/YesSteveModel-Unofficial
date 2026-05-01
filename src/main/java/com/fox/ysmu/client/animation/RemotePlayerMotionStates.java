package com.fox.ysmu.client.animation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayer;

import com.fox.ysmu.data.PlayerMotionState;

public final class RemotePlayerMotionStates {

    private static final Map<UUID, Byte> STATES = new ConcurrentHashMap<>();

    private RemotePlayerMotionStates() {}

    public static void update(UUID playerId, byte flags) {
        if (playerId != null) {
            STATES.put(playerId, flags);
        }
    }

    public static void clear() {
        STATES.clear();
    }

    public static boolean isOnGround(EntityPlayer player) {
        Byte flags = getFlags(player);
        return flags != null ? PlayerMotionState.isOnGround(flags) : player.onGround;
    }

    public static boolean isFlying(EntityPlayer player) {
        Byte flags = getFlags(player);
        return flags != null ? PlayerMotionState.isFlying(flags) : player.capabilities.isFlying;
    }

    private static Byte getFlags(EntityPlayer player) {
        return player == null ? null : STATES.get(player.getUniqueID());
    }
}
