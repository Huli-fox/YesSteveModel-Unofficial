package com.fox.ysmu.model.format;

import static com.fox.ysmu.model.ServerModelManager.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.util.ModelIdUtil;
import com.google.common.collect.Maps;

import software.bernie.geckolib3.geo.raw.pojo.Converter;
import software.bernie.geckolib3.geo.raw.pojo.RawGeoModel;

public final class FolderFormat {

    public static void cacheAllModels(Path rootPath) {
        File root = rootPath.toFile();
        File[] dirs = root.listFiles(file -> file.isDirectory());
        if (dirs == null) {
            return;
        }
        for (File dir : dirs) {
            String dirName = dir.getName();
            boolean noMainModelFile = true;
            boolean noArmModelFile = true;
            boolean noTextureFile = true;
            Collection<File> files = FileUtils.listFiles(
                dir,
                FileFileFilter.FILE,
                null);
            for (File file : files) {
                String fileName = file.getName();
                if (MAIN_MODEL_FILE_NAME.equals(fileName) && isNotBlankFile(file)) {
                    noMainModelFile = false;
                }
                if (ARM_MODEL_FILE_NAME.equals(fileName) && isNotBlankFile(file)) {
                    noArmModelFile = false;
                }
                if (fileName.endsWith(".png")) {
                    noTextureFile = false;
                }
            }
            if (noMainModelFile) {
                continue;
            }
            if (noArmModelFile) {
                continue;
            }
            if (noTextureFile) {
                continue;
            }
            String modelId = ModelIdUtil.getInternalModelId(dirName);
            ServerModelInfo info = cacheModel(dir.toPath(), modelId);
            if (info != null) {
                CACHE_NAME_INFO.put(modelId, info);
            }
        }
    }

    private static ServerModelInfo cacheModel(Path modelPath, String modelId) {
        try {
            ModelData data = getModelDataFromPath(modelPath, modelId);
            return ModelCacheWriter.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @NotNull
    public static ModelData getModelData(Path rootPath, String modelName) throws IOException {
        return getModelDataFromPath(rootPath.resolve(modelName), ModelIdUtil.getInternalModelId(modelName));
    }

    @NotNull
    private static ModelData getModelDataFromPath(Path modelPath, String modelId) throws IOException {
        Map<String, byte[]> model = Maps.newHashMap();
        model.put("main", getBytes(modelPath, MAIN_MODEL_FILE_NAME));
        model.put("arm", getBytes(modelPath, ARM_MODEL_FILE_NAME));

        Map<String, byte[]> texture = Maps.newHashMap();
        Collection<File> textures = FileUtils.listFiles(modelPath.toFile(), new String[] { "png" }, false);
        for (File png : textures) {
            String fileName = png.getName();
            texture.put(fileName, getBytes(modelPath, fileName));
        }

        Map<String, byte[]> animation = Maps.newHashMap();
        animation.put("main", getBytes(modelPath, MAIN_ANIMATION_FILE_NAME));
        animation.put("arm", getBytes(modelPath, ARM_ANIMATION_FILE_NAME));
        animation.put("extra", getBytes(modelPath, EXTRA_ANIMATION_FILE_NAME));

        return new ModelData(modelId, Type.FOLDER, model, texture, animation);
    }

    private static byte[] getBytes(Path root, String fileName) throws IOException {
        Path filePath = root.resolve(fileName);
        if (MAIN_ANIMATION_FILE_NAME.equals(fileName) && !filePath.toFile()
            .isFile()) {
            filePath = CUSTOM.resolve("default/main.animation.json");
        }
        if (ARM_ANIMATION_FILE_NAME.equals(fileName) && !filePath.toFile()
            .isFile()) {
            filePath = CUSTOM.resolve("default/arm.animation.json");
        }
        if (EXTRA_ANIMATION_FILE_NAME.equals(fileName) && !filePath.toFile()
            .isFile()) {
            filePath = CUSTOM.resolve("default/extra.animation.json");
        }

        if (MAIN_MODEL_FILE_NAME.equals(fileName) || ARM_MODEL_FILE_NAME.equals(fileName)) {
            String modelJson = FileUtils.readFileToString(filePath.toFile(), StandardCharsets.UTF_8);
            RawGeoModel rawModel = Converter.fromJsonString(modelJson);
            // 直接返回JSON字符串的字节数组，而不是尝试序列化RawGeoModel对象
            return modelJson.getBytes(StandardCharsets.UTF_8);
        }

        return FileUtils.readFileToByteArray(filePath.toFile());
    }

    private static boolean isNotBlankFile(File file) {
        try {
            String fileText = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            return StringUtils.isNoneBlank(fileText);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
