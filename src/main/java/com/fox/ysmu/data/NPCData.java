package com.fox.ysmu.data;

import java.util.Map;
import java.util.UUID;

import net.minecraft.util.ResourceLocation;

import com.google.common.collect.Maps;

import it.unimi.dsi.fastutil.Pair;

public final class NPCData {

    private static Map<UUID, Pair<ResourceLocation, ResourceLocation>> DATA = Maps.newHashMap();

    public static void clear() {
        DATA.clear();
    }

    public static void addAll(Map<UUID, Pair<ResourceLocation, ResourceLocation>> data) {
        DATA = data;
    }

    public static void put(UUID uuid, ResourceLocation modelId, ResourceLocation textureId) {
        DATA.put(uuid, Pair.of(modelId, textureId));
    }

    public static boolean contains(UUID uuid) {
        return DATA.containsKey(uuid);
    }

    public static Pair<ResourceLocation, ResourceLocation> getData(UUID uuid) {
        return DATA.get(uuid);
    }
}
