package com.fox.ysmu.client.animation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import com.fox.ysmu.client.entity.CustomPlayerEntity;

import software.bernie.geckolib3.core.event.predicate.AnimationEvent;

public final class RemotePlayerAnimationQueries {

    private static final float MAX_HEAD_YAW = 85.0F;
    private static final float MAX_GROUND_SPEED = 12.0F;
    private static final float MAX_YAW_SPEED = 45.0F;
    private static final float HEAD_YAW_RESPONSE = 0.45F;
    private static final float GROUND_SPEED_RESPONSE = 0.35F;
    private static final float YAW_SPEED_RESPONSE = 0.50F;
    private static final float RESET_AFTER_TICKS = 20.0F;
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private RemotePlayerAnimationQueries() {}

    public static QueryValues get(AnimationEvent<CustomPlayerEntity> animationEvent, EntityPlayer player,
        float rawHeadYaw) {
        if (player == null) {
            return new QueryValues(rawHeadYaw, 0.0F, 0.0F);
        }
        float groundSpeed = getGroundSpeed(player);
        float yawSpeed = getYawSpeed(player);
        if (!isRemotePlayer(player)) {
            return new QueryValues(rawHeadYaw, groundSpeed, yawSpeed);
        }

        UUID id = player.getUniqueID();
        if (id == null) {
            return new QueryValues(clampHeadYaw(rawHeadYaw), clampGroundSpeed(groundSpeed), clampYawSpeed(yawSpeed));
        }

        float renderTick = player.ticksExisted + animationEvent.getPartialTick();
        State state = STATES.computeIfAbsent(id, ignored -> new State());
        return state.update(renderTick, clampHeadYaw(rawHeadYaw), clampGroundSpeed(groundSpeed), clampYawSpeed(yawSpeed));
    }

    public static void clear() {
        STATES.clear();
    }

    private static boolean isRemotePlayer(EntityPlayer player) {
        return player != null && player != Minecraft.getMinecraft().thePlayer;
    }

    private static float getGroundSpeed(EntityPlayer player) {
        double dx;
        double dz;
        if (isRemotePlayer(player)) {
            dx = player.posX - player.prevPosX;
            dz = player.posZ - player.prevPosZ;
        } else {
            dx = player.motionX;
            dz = player.motionZ;
        }
        return (float) (MathHelper.sqrt_double(dx * dx + dz * dz) * 20.0D);
    }

    private static float getYawSpeed(EntityPlayer player) {
        return MathHelper.wrapAngleTo180_float(player.rotationYaw - player.prevRotationYaw);
    }

    private static float clampHeadYaw(float value) {
        return MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(value), -MAX_HEAD_YAW, MAX_HEAD_YAW);
    }

    private static float clampGroundSpeed(float value) {
        return MathHelper.clamp_float(value, 0.0F, MAX_GROUND_SPEED);
    }

    private static float clampYawSpeed(float value) {
        return MathHelper.clamp_float(value, -MAX_YAW_SPEED, MAX_YAW_SPEED);
    }

    private static float smooth(float current, float target, float elapsedTicks, float responsePerTick) {
        if (elapsedTicks <= 0.0F) {
            return current;
        }
        float alpha = 1.0F - (float) Math.pow(1.0F - responsePerTick, elapsedTicks);
        alpha = MathHelper.clamp_float(alpha, 0.0F, 1.0F);
        return current + (target - current) * alpha;
    }

    public static final class QueryValues {

        private final float headYaw;
        private final float groundSpeed;
        private final float yawSpeed;

        private QueryValues(float headYaw, float groundSpeed, float yawSpeed) {
            this.headYaw = headYaw;
            this.groundSpeed = groundSpeed;
            this.yawSpeed = yawSpeed;
        }

        public float headYaw() {
            return headYaw;
        }

        public float groundSpeed() {
            return groundSpeed;
        }

        public float yawSpeed() {
            return yawSpeed;
        }
    }

    private static final class State {

        private boolean initialized;
        private float lastRenderTick;
        private float headYaw;
        private float groundSpeed;
        private float yawSpeed;

        private QueryValues update(float renderTick, float targetHeadYaw, float targetGroundSpeed,
            float targetYawSpeed) {
            if (!initialized) {
                initialize(renderTick, targetHeadYaw, targetGroundSpeed, targetYawSpeed);
                return values();
            }

            float elapsedTicks = renderTick - lastRenderTick;
            if (elapsedTicks < 0.0F || elapsedTicks > RESET_AFTER_TICKS) {
                initialize(renderTick, targetHeadYaw, targetGroundSpeed, targetYawSpeed);
                return values();
            }

            headYaw = smooth(headYaw, targetHeadYaw, elapsedTicks, HEAD_YAW_RESPONSE);
            groundSpeed = smooth(groundSpeed, targetGroundSpeed, elapsedTicks, GROUND_SPEED_RESPONSE);
            yawSpeed = smooth(yawSpeed, targetYawSpeed, elapsedTicks, YAW_SPEED_RESPONSE);
            lastRenderTick = renderTick;
            return values();
        }

        private void initialize(float renderTick, float targetHeadYaw, float targetGroundSpeed, float targetYawSpeed) {
            initialized = true;
            lastRenderTick = renderTick;
            headYaw = targetHeadYaw;
            groundSpeed = targetGroundSpeed;
            yawSpeed = targetYawSpeed;
        }

        private QueryValues values() {
            return new QueryValues(headYaw, groundSpeed, yawSpeed);
        }
    }
}
