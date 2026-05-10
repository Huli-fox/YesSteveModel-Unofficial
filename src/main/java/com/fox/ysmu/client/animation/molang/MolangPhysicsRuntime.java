package com.fox.ysmu.client.animation.molang;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.fox.ysmu.client.entity.CustomPlayerEntity;

import software.bernie.geckolib3.core.molang.MolangStringPool;
import software.bernie.geckolib3.core.processor.AnimationProcessor;
import software.bernie.geckolib3.core.processor.IBone;

public final class MolangPhysicsRuntime {

    private static final ThreadLocal<FrameContext> CURRENT = new ThreadLocal<>();
    private static final Map<ScopeKey, ScopeState> STATES = new ConcurrentHashMap<>();

    private MolangPhysicsRuntime() {}

    public static void begin(CustomPlayerEntity animatable, double renderTicks, AnimationProcessor<?> processor) {
        if (animatable == null || processor == null) {
            CURRENT.remove();
            return;
        }
        EntityPlayer player = animatable.getPlayer();
        ScopeKey key = ScopeKey.from(player, animatable.getMainModel(), animatable.getAnimation());
        ScopeState state = STATES.computeIfAbsent(key, ignored -> new ScopeState());
        state.physics.update(renderTicks);
        CURRENT.set(new FrameContext(state, processor));
    }

    public static void end() {
        CURRENT.remove();
    }

    public static void clear() {
        STATES.clear();
        CURRENT.remove();
    }

    public static double firstOrder(int nameId, double input, double response) {
        if (nameId == MolangStringPool.EMPTY_ID) {
            return 0.0D;
        }
        FrameContext context = CURRENT.get();
        if (context == null) {
            return input;
        }
        return context.state.physics.firstOrder(nameId, input, response);
    }

    public static double secondOrder(int nameId, double input, double frequency, double coefficient, double response) {
        if (nameId == MolangStringPool.EMPTY_ID) {
            return 0.0D;
        }
        FrameContext context = CURRENT.get();
        if (context == null) {
            return input;
        }
        return context.state.physics.secondOrder(nameId, input, frequency, coefficient, response);
    }

    public static double getVariable(String name, double fallback) {
        FrameContext context = CURRENT.get();
        if (context == null) {
            return fallback;
        }
        Double value = context.state.variables.get(name);
        return value == null ? fallback : value;
    }

    public static boolean setVariable(String name, double value) {
        FrameContext context = CURRENT.get();
        if (context == null) {
            return false;
        }
        context.state.variables.put(name, value);
        return true;
    }

    public static double boneRotation(int nameId, char axis) {
        IBone bone = bone(nameId);
        if (bone == null) {
            return 0.0D;
        }
        if (axis == 'x') {
            return -Math.toDegrees(bone.getRotationX());
        }
        if (axis == 'y') {
            return -Math.toDegrees(bone.getRotationY());
        }
        return Math.toDegrees(bone.getRotationZ());
    }

    public static double bonePosition(int nameId, char axis) {
        IBone bone = bone(nameId);
        if (bone == null) {
            return 0.0D;
        }
        if (axis == 'x') {
            return bone.getPositionX();
        }
        if (axis == 'y') {
            return bone.getPositionY();
        }
        return bone.getPositionZ();
    }

    public static double boneScale(int nameId, char axis) {
        IBone bone = bone(nameId);
        if (bone == null) {
            return axis == 'x' || axis == 'y' || axis == 'z' ? 1.0D : 0.0D;
        }
        if (axis == 'x') {
            return bone.getScaleX();
        }
        if (axis == 'y') {
            return bone.getScaleY();
        }
        return bone.getScaleZ();
    }

    private static IBone bone(int nameId) {
        FrameContext context = CURRENT.get();
        if (context == null || nameId == MolangStringPool.EMPTY_ID) {
            return null;
        }
        String boneName = MolangStringPool.get(nameId);
        return boneName == null ? null : context.processor.getBone(boneName);
    }

    private static final class FrameContext {
        private final ScopeState state;
        private final AnimationProcessor<?> processor;

        private FrameContext(ScopeState state, AnimationProcessor<?> processor) {
            this.state = state;
            this.processor = processor;
        }
    }

    private static final class ScopeState {
        private final MolangPhysicsState physics = new MolangPhysicsState();
        private final Map<String, Double> variables = new ConcurrentHashMap<>();
    }

    private static final class ScopeKey {
        private final UUID playerId;
        private final ResourceLocation modelId;
        private final ResourceLocation animationId;
        private final int fallbackIdentity;

        private ScopeKey(UUID playerId, ResourceLocation modelId, ResourceLocation animationId, int fallbackIdentity) {
            this.playerId = playerId;
            this.modelId = modelId;
            this.animationId = animationId;
            this.fallbackIdentity = fallbackIdentity;
        }

        private static ScopeKey from(EntityPlayer player, ResourceLocation modelId, ResourceLocation animationId) {
            UUID playerId = player == null ? null : player.getUniqueID();
            int fallbackIdentity = playerId == null
                ? 31 * System.identityHashCode(modelId) + System.identityHashCode(animationId)
                : 0;
            return new ScopeKey(playerId, modelId, animationId, fallbackIdentity);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ScopeKey)) {
                return false;
            }
            ScopeKey other = (ScopeKey) obj;
            if (fallbackIdentity != other.fallbackIdentity) {
                return false;
            }
            if (playerId == null ? other.playerId != null : !playerId.equals(other.playerId)) {
                return false;
            }
            if (modelId == null ? other.modelId != null : !modelId.equals(other.modelId)) {
                return false;
            }
            return animationId == null ? other.animationId == null : animationId.equals(other.animationId);
        }

        @Override
        public int hashCode() {
            int result = playerId == null ? 0 : playerId.hashCode();
            result = 31 * result + (modelId == null ? 0 : modelId.hashCode());
            result = 31 * result + (animationId == null ? 0 : animationId.hashCode());
            result = 31 * result + fallbackIdentity;
            return result;
        }
    }
}
