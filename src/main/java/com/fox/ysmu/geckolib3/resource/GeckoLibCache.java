package com.fox.ysmu.geckolib3.resource;

import com.fox.ysmu.geckolib3.core.molang.MolangParser;
import com.fox.ysmu.geckolib3.file.AnimationFile;
import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.google.common.collect.Maps;
import net.minecraft.util.ResourceLocation;

import java.util.Map;

public class GeckoLibCache {
    private static GeckoLibCache INSTANCE;
    public final MolangParser parser = new MolangParser();
    private final Map<ResourceLocation, AnimationFile> animations = Maps.newHashMap();
    private final Map<ResourceLocation, GeoModel> geoModels = Maps.newHashMap();

    public static GeckoLibCache getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GeckoLibCache();
            return INSTANCE;
        }
        return INSTANCE;
    }

    public Map<ResourceLocation, AnimationFile> getAnimations() {
        return animations;
    }

    public Map<ResourceLocation, GeoModel> getGeoModels() {
        return geoModels;
    }
}
