package com.fox.ysmu.util;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class GsonHelper {

    // 默认的 Gson 实例，用于不指定 Gson 对象的调用
    private static final Gson DEFAULT_GSON = new Gson();

    /**
     * 使用指定的 Gson 实例将 Json 字符串反序列化为指定类型的对象。
     *
     * @param gson     用于反序列化的 Gson 实例
     * @param json     要反序列化的 Json 字符串
     * @param classOfT 目标对象的类
     * @return 反序列化后的对象，如果 json 为 null 则返回 null
     * @throws JsonSyntaxException 如果 json 格式错误
     */
    @Nullable
    public static <T> T fromJson(Gson gson, String json, Class<? extends T> classOfT) throws JsonSyntaxException {
        if (json == null) {
            return null;
        }
        // 使用传入的 gson 实例进行转换
        return gson.fromJson(json, classOfT);
    }

    /**
     * 使用默认的 Gson 实例将 JsonElement 反序列化为指定类型的对象。
     */
    @Nullable
    public static <T> T fromJson(JsonElement json, Class<? extends T> classOfT) throws JsonSyntaxException {
        if (json == null || json.isJsonNull()) {
            return null;
        }
        return DEFAULT_GSON.fromJson(json, classOfT);
    }

    /**
     * 使用默认的 Gson 实例将 Json 字符串反序列化为指定类型的对象。
     */
    @Nullable
    public static <T> T fromJson(String json, Class<? extends T> classOfT) throws JsonSyntaxException {
        if (json == null) {
            return null;
        }
        return DEFAULT_GSON.fromJson(json, classOfT);
    }

    /**
     * 从 JsonObject 中获取一个字符串。
     */
    public static String getAsString(JsonObject jsonObject, String memberName) {
        if (jsonObject.has(memberName)) {
            JsonElement element = jsonObject.get(memberName);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive()
                .isString()) {
                return element.getAsString();
            } else {
                throw new JsonSyntaxException(
                    "Expected '" + memberName + "' to be a String, was " + getElementType(element));
            }
        } else {
            throw new JsonSyntaxException("Missing '" + memberName + "', expected to find a String");
        }
    }

    /**
     * 从 JsonObject 中获取一个布尔值。
     */
    public static boolean getAsBoolean(JsonObject jsonObject, String memberName) {
        if (jsonObject.has(memberName)) {
            JsonElement element = jsonObject.get(memberName);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive()
                .isBoolean()) {
                return element.getAsBoolean();
            } else {
                throw new JsonSyntaxException(
                    "Expected '" + memberName + "' to be a Boolean, was " + getElementType(element));
            }
        } else {
            throw new JsonSyntaxException("Missing '" + memberName + "', expected to find a Boolean");
        }
    }

    /**
     * 从 JsonObject 中获取一个整数。
     */
    public static int getAsInt(JsonObject jsonObject, String memberName) {
        if (jsonObject.has(memberName)) {
            JsonElement element = jsonObject.get(memberName);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive()
                .isNumber()) {
                return element.getAsInt();
            } else {
                throw new JsonSyntaxException(
                    "Expected '" + memberName + "' to be a Int, was " + getElementType(element));
            }
        } else {
            throw new JsonSyntaxException("Missing '" + memberName + "', expected to find a Int");
        }
    }

    /**
     * 获取 JsonElement 的类型名称。
     */
    private static String getElementType(@Nullable JsonElement jsonElement) {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return "NULL";
        }
        if (jsonElement.isJsonPrimitive()) {
            return "primitive";
        }
        if (jsonElement.isJsonArray()) {
            return "array";
        }
        if (jsonElement.isJsonObject()) {
            return "object";
        }
        return "unknown";
    }
}
