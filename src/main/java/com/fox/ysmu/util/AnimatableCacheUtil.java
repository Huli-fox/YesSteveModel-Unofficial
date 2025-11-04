package com.fox.ysmu.util;

import software.bernie.geckolib3.core.IAnimatable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.concurrent.TimeUnit;

@OnlyIn(Dist.CLIENT)
public final class AnimatableCacheUtil {
    public static final Cache<ResourceLocation, IAnimatable> ANIMATABLE_CACHE = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();
    public static final Cache<ResourceLocation, IAnimatable> TEXTURE_GUI_CACHE = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();
    public static final Cache<ResourceLocation, Entity> ENTITIES_CACHE = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();
}
