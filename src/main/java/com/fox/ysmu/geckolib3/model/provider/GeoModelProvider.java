package com.fox.ysmu.geckolib3.model.provider;

import net.minecraft.util.ResourceLocation;
import com.fox.ysmu.geckolib3.GeckoLib;
import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.fox.ysmu.geckolib3.resource.GeckoLibCache;

public abstract class GeoModelProvider<T> {
    public double seekTime;
    public double lastGameTickTime;
    public boolean shouldCrashOnMissing = false;

    public GeoModel getModel(ResourceLocation location) {
        if (GeckoLibCache.getInstance().getGeoModels().containsKey(location)) {
            return GeckoLibCache.getInstance().getGeoModels().get(location);
        }
        return GeckoLibCache.getInstance().getGeoModels().get(new ResourceLocation(GeckoLib.ModID, "geo/testdiagonal2.geo.json"));
    }

    public abstract ResourceLocation getModelLocation(T object);

    public abstract ResourceLocation getTextureLocation(T object);
}
