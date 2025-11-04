package com.fox.ysmu.model.format;

import static com.fox.ysmu.model.ServerModelManager.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import com.fox.ysmu.compat.Utils;
import com.fox.ysmu.data.EncryptTools;
import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.util.Md5Utils;
import com.fox.ysmu.util.YesModelUtils;
import com.google.common.collect.Maps;

import software.bernie.geckolib3.geo.raw.pojo.Converter;
import software.bernie.geckolib3.geo.raw.pojo.RawGeoModel;

public final class YsmFormat {

    public static void cacheAllModels(Path rootPath) {
        Collection<File> ysmFiles = FileUtils.listFiles(rootPath.toFile(), new String[] { "ysm" }, false);
        for (File ysmFile : ysmFiles) {
            String modelId = removeExtension(ysmFile.getName());
            if (!Utils.isValidResourceLocation(modelId)) {
                continue;
            }
            try {
                Map<String, byte[]> data = YesModelUtils.input(ysmFile);
                if (data.isEmpty()) {
                    continue;
                }
                if (!data.containsKey(MAIN_MODEL_FILE_NAME)) {
                    continue;
                }
                if (!data.containsKey(ARM_MODEL_FILE_NAME)) {
                    continue;
                }
                if (data.keySet()
                    .stream()
                    .noneMatch(fileName -> fileName.endsWith(".png"))) {
                    continue;
                }

                if (rootPath.equals(AUTH)) {
                    ServerModelInfo info = cacheModel(data, modelId, true);
                    if (info != null) {
                        CACHE_NAME_INFO.put(modelId, info);
                        AUTH_MODELS.add(modelId);
                    }
                } else {
                    ServerModelInfo info = cacheModel(data, modelId, false);
                    if (info != null) {
                        CACHE_NAME_INFO.put(modelId, info);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static ServerModelInfo cacheModel(Map<String, byte[]> input, String modelId, boolean isAuth) {
        try {
            ModelData data = getModelData(input, modelId, isAuth);
            byte[] dataBytes = EncryptTools.assembleEncryptModels(data);
            data.setMd5(
                Md5Utils.md5Hex(dataBytes)
                    .toUpperCase(Locale.US));
            FileUtils.writeByteArrayToFile(
                CACHE_SERVER.resolve(
                    data.getInfo()
                        .getMd5())
                    .toFile(),
                dataBytes);
            return data.getInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @NotNull
    private static ModelData getModelData(Map<String, byte[]> data, String modelId, boolean isAuth) throws IOException {
        Map<String, byte[]> model = Maps.newHashMap();
        model.put("main", getBytes(data, MAIN_MODEL_FILE_NAME));
        model.put("arm", getBytes(data, ARM_MODEL_FILE_NAME));

        Map<String, byte[]> texture = Maps.newHashMap();
        data.forEach((name, textureData) -> {
            if (name.endsWith(".png")) {
                texture.put(name, textureData);
            }
        });

        Map<String, byte[]> animation = Maps.newHashMap();
        animation.put("main", getBytes(data, MAIN_ANIMATION_FILE_NAME));
        animation.put("arm", getBytes(data, ARM_ANIMATION_FILE_NAME));
        animation.put("extra", getBytes(data, EXTRA_ANIMATION_FILE_NAME));

        return new ModelData(modelId, isAuth, Type.YSM, model, texture, animation);
    }

    private static byte[] getBytes(Map<String, byte[]> data, String fileName) throws IOException {
        if (MAIN_ANIMATION_FILE_NAME.equals(fileName) && !data.containsKey(MAIN_ANIMATION_FILE_NAME)) {
            Path filePath = CUSTOM.resolve("default/main.animation.json");
            return FileUtils.readFileToByteArray(filePath.toFile());
        }
        if (ARM_ANIMATION_FILE_NAME.equals(fileName) && !data.containsKey(ARM_ANIMATION_FILE_NAME)) {
            Path filePath = CUSTOM.resolve("default/arm.animation.json");
            return FileUtils.readFileToByteArray(filePath.toFile());
        }
        if (EXTRA_ANIMATION_FILE_NAME.equals(fileName) && !data.containsKey(EXTRA_ANIMATION_FILE_NAME)) {
            Path filePath = CUSTOM.resolve("default/extra.animation.json");
            return FileUtils.readFileToByteArray(filePath.toFile());
        }

        if (MAIN_MODEL_FILE_NAME.equals(fileName) || ARM_MODEL_FILE_NAME.equals(fileName)) {
            String modelJson = new String(data.get(fileName), StandardCharsets.UTF_8);
            RawGeoModel rawModel = Converter.fromJsonString(modelJson);
            // 直接返回JSON字符串的字节数组，而不是尝试序列化RawGeoModel对象
            return modelJson.getBytes(StandardCharsets.UTF_8);
        }

        return data.get(fileName);
    }
}
