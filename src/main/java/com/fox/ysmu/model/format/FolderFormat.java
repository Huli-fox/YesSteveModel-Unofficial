package com.fox.ysmu.model.format;


import com.fox.ysmu.data.EncryptTools;
import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.geckolib3.geo.raw.pojo.Converter;
import com.fox.ysmu.geckolib3.geo.raw.pojo.RawGeoModel;
import com.fox.ysmu.util.Md5Utils;
import com.fox.ysmu.util.ObjectStreamUtil;
import com.fox.ysmu.data.ModelData;
import com.google.common.collect.Maps;
//import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import static com.fox.ysmu.model.ServerModelManager.*;

public final class FolderFormat {
    public static void cacheAllModels(Path rootPath) {
        Collection<File> dirs = FileUtils.listFiles(rootPath.toFile(), DirectoryFileFilter.INSTANCE, null);
        for (File dir : dirs) {
            String dirName = dir.getName();
            if (!ResourceLocation.isValidResourceLocation(dirName)) {
                continue;
            }
            boolean noMainModelFile = true;
            boolean noArmModelFile = true;
            boolean noTextureFile = true;
            Collection<File> files = FileUtils.listFiles(rootPath.resolve(dirName).toFile(), FileFileFilter.INSTANCE, null);
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
            if (rootPath.equals(AUTH)) {
                ServerModelInfo info = cacheModel(AUTH, dirName, true);
                if (info != null) {
                    CACHE_NAME_INFO.put(dirName, info);
                    AUTH_MODELS.add(dirName);
                }
            } else {
                ServerModelInfo info = cacheModel(CUSTOM, dirName, false);
                if (info != null) {
                    CACHE_NAME_INFO.put(dirName, info);
                }
            }
        }
    }

    private static ServerModelInfo cacheModel(Path rootPath, String modelId, boolean isAuth) {
        try {
            ModelData data = getModelData(rootPath, modelId, isAuth);
            byte[] dataBytes = EncryptTools.assembleEncryptModels(data);
            data.setMd5(Md5Utils.md5Hex(dataBytes).toUpperCase(Locale.US));
            FileUtils.writeByteArrayToFile(CACHE_SERVER.resolve(data.getInfo().getMd5()).toFile(), dataBytes);
            return data.getInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @NotNull
    public static ModelData getModelData(Path rootPath, String modelId, boolean isAuth) throws IOException {
        Path modelPath = rootPath.resolve(modelId);

        Map<String, byte[]> model = Maps.newHashMap();
        model.put("main", getBytes(modelPath, MAIN_MODEL_FILE_NAME));
        model.put("arm", getBytes(modelPath, ARM_MODEL_FILE_NAME));


        Map<String, byte[]> texture = Maps.newHashMap();
        Collection<File> textures = FileUtils.listFiles(modelPath.toFile(), new String[]{"png"}, false);
        for (File png : textures) {
            String fileName = png.getName();
            texture.put(fileName, getBytes(modelPath, fileName));
        }

        Map<String, byte[]> animation = Maps.newHashMap();
        animation.put("main", getBytes(modelPath, MAIN_ANIMATION_FILE_NAME));
        animation.put("arm", getBytes(modelPath, ARM_ANIMATION_FILE_NAME));
        animation.put("extra", getBytes(modelPath, EXTRA_ANIMATION_FILE_NAME));

        return new ModelData(modelId, isAuth, Type.FOLDER, model, texture, animation);
    }

    private static byte[] getBytes(Path root, String fileName) throws IOException {
        Path filePath = root.resolve(fileName);
        if (MAIN_ANIMATION_FILE_NAME.equals(fileName) && !filePath.toFile().isFile()) {
            filePath = CUSTOM.resolve("default/main.animation.json");
        }
        if (ARM_ANIMATION_FILE_NAME.equals(fileName) && !filePath.toFile().isFile()) {
            filePath = CUSTOM.resolve("default/arm.animation.json");
        }
        if (EXTRA_ANIMATION_FILE_NAME.equals(fileName) && !filePath.toFile().isFile()) {
            filePath = CUSTOM.resolve("default/extra.animation.json");
        }

        if (MAIN_MODEL_FILE_NAME.equals(fileName) || ARM_MODEL_FILE_NAME.equals(fileName)) {
            String modelJson = FileUtils.readFileToString(filePath.toFile(), StandardCharsets.UTF_8);
            RawGeoModel rawModel = Converter.fromJsonString(modelJson);
            return ObjectStreamUtil.toByteArray(rawModel);
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
