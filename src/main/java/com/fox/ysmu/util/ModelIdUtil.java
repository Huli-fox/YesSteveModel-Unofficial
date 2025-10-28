package com.fox.ysmu.util;

import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public final class ModelIdUtil {
    public static ResourceLocation getSubModelId(ResourceLocation id, String subName) {
        String newPath = id.getPath() + "/" + subName;
        return new ResourceLocation(id.getNamespace(), newPath);
    }

    public static ResourceLocation getMainId(ResourceLocation id) {
        return getSubModelId(id, "main");
    }

    public static ResourceLocation getArmId(ResourceLocation id) {
        return getSubModelId(id, "arm");
    }

    public static ResourceLocation getModelIdFromMainId(ResourceLocation mainId) {
        String newPath = mainId.getPath().substring(0, mainId.getPath().length() - 5);
        return new ResourceLocation(mainId.getNamespace(), newPath);
    }

    @Nullable
    public static String getSubNameFromId(ResourceLocation mainId) {
        String[] split = mainId.getPath().split("/", 2);
        if (split.length == 2) {
            return split[1];
        }
        return StringUtils.EMPTY;
    }
}
