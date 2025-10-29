package com.fox.ysmu.geckolib3.model.provider;

import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.fox.ysmu.geckolib3.resource.GeckoLibCache;
import net.minecraft.util.ResourceLocation;

public abstract class GeoModelProvider<T> {
    public double seekTime;
    public double lastGameTickTime;
    public boolean shouldCrashOnMissing = false;

    public GeoModel getModel(ResourceLocation location) {
        return GeckoLibCache.getInstance().getGeoModels().get(location);
    }

    public abstract ResourceLocation getModelLocation(T object);

    public abstract ResourceLocation getTextureLocation(T object);
}
