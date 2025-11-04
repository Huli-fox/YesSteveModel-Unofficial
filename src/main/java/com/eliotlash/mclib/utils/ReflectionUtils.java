package com.eliotlash.mclib.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import software.bernie.example.config.ConfigHandler;

public class ReflectionUtils {

    /**
     * Minecraft texture manager's field to the texture map (a map of
     * {@link ITextureObject} which is used to cache references to
     * OpenGL textures).
     */
    public static Field TEXTURE_MAP;

    /**
     * Whether isShadowPass field was checked
     */
    private static boolean SHADOW_PASS_CHECK;

    /**
     * Optifine's shadow pass field
     */
    private static Field SHADOW_PASS;

    /**
     * Get texture map from texture manager using reflection API
     */
    @SuppressWarnings("unchecked")
    public static Map<ResourceLocation, ITextureObject> getTextures(TextureManager manager) {
        if (TEXTURE_MAP == null) {
            setupTextureMapField(manager);
        }

        try {
            return (Map<ResourceLocation, ITextureObject>) TEXTURE_MAP.get(manager);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Setup texture map field which is looked up using the reflection API
     */
    @SuppressWarnings("rawtypes")
    public static void setupTextureMapField(TextureManager manager) {
        /* Finding the field which has holds the texture cache */
        for (Field field : manager.getClass()
            .getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);

            try {
                Object value = field.get(manager);

                if (value instanceof Map && ((Map) value).keySet()
                    .iterator()
                    .next() instanceof ResourceLocation) {
                    TEXTURE_MAP = field;

                    break;
                }
            } catch (Exception e) {
                if (ConfigHandler.debugPrintStacktraces) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static boolean registerResourcePack(IResourcePack pack) {
        try {
            Field field = FMLClientHandler.class.getDeclaredField("resourcePackList");
            field.setAccessible(true);

            List<IResourcePack> packs = (List<IResourcePack>) field.get(FMLClientHandler.instance());
            packs.add(pack);
            IResourceManager manager = Minecraft.getMinecraft()
                .getResourceManager();

            if (manager instanceof SimpleReloadableResourceManager) {
                ((SimpleReloadableResourceManager) manager).reloadResourcePack(pack);
            }

            return false;
        } catch (Exception e) {
            if (ConfigHandler.debugPrintStacktraces) {
                e.printStackTrace();
            }
        }

        return false;
    }

    @Deprecated
    public static boolean isOptifineShadowPass() {
        if (!SHADOW_PASS_CHECK) {
            try {
                Class clazz = Class.forName("net.optifine.shaders.Shaders");

                SHADOW_PASS = clazz.getDeclaredField("isShadowPass");
            } catch (Exception e) {}

            SHADOW_PASS_CHECK = true;
        }

        if (SHADOW_PASS != null) {
            try {
                return (boolean) SHADOW_PASS.get(null);
            } catch (Exception e) {}
        }

        return false;
    }
}
