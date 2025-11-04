package com.fox.ysmu.util;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.StringUtils;

public final class ModelIdUtil {

    public static ResourceLocation getSubModelId(ResourceLocation id, String subName) {
        String newPath = id.getResourcePath() + "/" + subName;
        return new ResourceLocation(id.getResourceDomain(), newPath);
    }

    public static ResourceLocation getMainId(ResourceLocation id) {
        return getSubModelId(id, "main");
    }

    public static ResourceLocation getArmId(ResourceLocation id) {
        return getSubModelId(id, "arm");
    }

    public static ResourceLocation getModelIdFromMainId(ResourceLocation mainId) {
        String newPath = mainId.getResourcePath()
            .substring(
                0,
                mainId.getResourcePath()
                    .length() - 5);
        return new ResourceLocation(mainId.getResourceDomain(), newPath);
    }

    @Nullable
    public static String getSubNameFromId(ResourceLocation mainId) {
        String[] split = mainId.getResourcePath()
            .split("/", 2);
        if (split.length == 2) {
            return split[1];
        }
        return StringUtils.EMPTY;
    }
}
