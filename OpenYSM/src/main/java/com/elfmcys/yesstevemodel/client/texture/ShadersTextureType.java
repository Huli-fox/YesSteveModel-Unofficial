package com.elfmcys.yesstevemodel.client.texture;

import net.minecraft.resources.ResourceLocation;

public enum ShadersTextureType {
    NORMAL("_n"),
    SPECULAR("_s");

    public static final ShadersTextureType[] VALUES = values();

    private final String suffix;

    ShadersTextureType(String suffix) {
        this.suffix = suffix;
    }

    public ResourceLocation appendSuffix(ResourceLocation resourceLocation) {
        return ResourceLocation.fromNamespaceAndPath(resourceLocation.getNamespace(), resourceLocation.getPath() + this.suffix);
    }
}
