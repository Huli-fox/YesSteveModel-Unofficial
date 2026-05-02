package com.fox.ysmu.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.StringUtils;

public final class ModelIdUtil {

    private static final String ENCODED_MODEL_PREFIX = "_name_";
    private static final Pattern SAFE_MODEL_ID = Pattern.compile("[a-z0-9._-]+");
    private static final char[] HEX = "0123456789abcdef".toCharArray();

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
        String path = mainId.getResourcePath();
        int separator = path.lastIndexOf('/');
        if (separator >= 0 && separator + 1 < path.length()) {
            return path.substring(separator + 1);
        }
        return StringUtils.EMPTY;
    }

    public static String getInternalModelId(String modelName) {
        if (StringUtils.isEmpty(modelName)) {
            return "unnamed";
        }
        if (SAFE_MODEL_ID.matcher(modelName).matches() && !modelName.startsWith(ENCODED_MODEL_PREFIX)) {
            return modelName;
        }
        return ENCODED_MODEL_PREFIX + encodeHex(modelName.getBytes(StandardCharsets.UTF_8));
    }

    public static String getModelDisplayName(ResourceLocation modelId) {
        return getModelDisplayName(modelId.getResourcePath());
    }

    public static String getModelDisplayName(String modelPath) {
        if (!modelPath.startsWith(ENCODED_MODEL_PREFIX)) {
            return modelPath;
        }
        String encoded = modelPath.substring(ENCODED_MODEL_PREFIX.length());
        if ((encoded.length() & 1) != 0) {
            return modelPath;
        }
        byte[] bytes = new byte[encoded.length() / 2];
        for (int i = 0; i < encoded.length(); i += 2) {
            int high = Character.digit(encoded.charAt(i), 16);
            int low = Character.digit(encoded.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                return modelPath;
            }
            bytes[i / 2] = (byte) ((high << 4) + low);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String encodeHex(byte[] bytes) {
        char[] output = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            output[i * 2] = HEX[value >>> 4];
            output[i * 2 + 1] = HEX[value & 0x0F];
        }
        return new String(output);
    }
}
