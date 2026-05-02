package com.fox.ysmu.client.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class AngelicaCompat {

    private static boolean initialized;
    private static boolean available;
    private static Object irisApi;
    private static Method isShaderPackInUse;
    private static Object handRenderer;
    private static Method isHandRendererActive;
    private static Method isRenderingSolid;
    private static Method isHandTranslucent;
    private static Object offHand;
    private static Method resetItemId;

    private AngelicaCompat() {}

    public static boolean usesShaderHandRenderer() {
        init();
        return available && isShaderPackInUse();
    }

    public static boolean isRenderingSolidHandPass() {
        init();
        return available
            && isShaderPackInUse()
            && invokeBoolean(handRenderer, isHandRendererActive)
            && invokeBoolean(handRenderer, isRenderingSolid);
    }

    public static boolean shouldRenderOffhandInCurrentHandPass() {
        init();
        if (!available || offHand == null || isHandTranslucent == null) {
            return true;
        }
        boolean offhandTranslucent = invokeBoolean(handRenderer, isHandTranslucent, offHand);
        return invokeBoolean(handRenderer, isRenderingSolid) != offhandTranslucent;
    }

    public static void resetFirstPersonItemId() {
        init();
        if (resetItemId != null) {
            try {
                resetItemId.invoke(null);
            } catch (ReflectiveOperationException ignored) {
                resetItemId = null;
            }
        }
    }

    private static boolean isShaderPackInUse() {
        return invokeBoolean(irisApi, isShaderPackInUse);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisApi = irisApiClass.getMethod("getInstance")
                .invoke(null);
            isShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");

            Class<?> handRendererClass = Class.forName("net.coderbot.iris.pipeline.HandRenderer");
            Field instance = handRendererClass.getField("INSTANCE");
            handRenderer = instance.get(null);
            isHandRendererActive = handRendererClass.getMethod("isActive");
            isRenderingSolid = handRendererClass.getMethod("isRenderingSolid");

            try {
                Class<?> interactionHand = Class.forName("com.gtnewhorizons.angelica.compat.mojang.InteractionHand");
                offHand = Enum.valueOf((Class<Enum>) interactionHand.asSubclass(Enum.class), "OFF_HAND");
                isHandTranslucent = handRendererClass.getMethod("isHandTranslucent", interactionHand);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                offHand = null;
                isHandTranslucent = null;
            }

            try {
                Class<?> itemIdManager = Class.forName("net.coderbot.iris.uniforms.ItemIdManager");
                resetItemId = itemIdManager.getMethod("resetItemId");
            } catch (ReflectiveOperationException | LinkageError ignored) {
                resetItemId = null;
            }
            available = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            available = false;
        }
    }

    private static boolean invokeBoolean(Object owner, Method method, Object... args) {
        if (owner == null || method == null) {
            return false;
        }
        try {
            Object value = method.invoke(owner, args);
            return value instanceof Boolean && (Boolean) value;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
