package com.fox.ysmu.model.format;

import com.fox.ysmu.data.EncryptTools;
import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.geckolib3.geo.raw.pojo.Converter;
import com.fox.ysmu.geckolib3.geo.raw.pojo.RawGeoModel;
import com.fox.ysmu.util.InputStreamUtils;
import com.fox.ysmu.util.Md5Utils;
import com.fox.ysmu.util.ObjectStreamUtil;
import com.google.common.collect.Maps;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.fox.ysmu.model.ServerModelManager.*;

public final class ZipFormat {
    public static void cacheAllModels(Path rootPath) {
        Collection<File> zipFiles = FileUtils.listFiles(rootPath.toFile(), new String[]{"zip"}, false);
        for (File file : zipFiles) {
            String modelId = removeExtension(file.getName());
            if (!ResourceLocation.isValidResourceLocation(modelId)) {
                continue;
            }
            try (ZipFile zipFile = new ZipFile(file)) {
                if (zipFile.getEntry(MAIN_MODEL_FILE_NAME) == null || isBlankEntry(zipFile, MAIN_MODEL_FILE_NAME)) {
                    continue;
                }
                if (zipFile.getEntry(ARM_MODEL_FILE_NAME) == null || isBlankEntry(zipFile, ARM_MODEL_FILE_NAME)) {
                    continue;
                }
                if (zipFile.stream().noneMatch(entry -> entry.getName().endsWith(".png"))) {
                    continue;
                }

                if (rootPath.equals(AUTH)) {
                    ServerModelInfo info = cacheModel(zipFile, modelId, true);
                    if (info != null) {
                        CACHE_NAME_INFO.put(modelId, info);
                        AUTH_MODELS.add(modelId);
                    }
                } else {
                    ServerModelInfo info = cacheModel(zipFile, modelId, false);
                    if (info != null) {
                        CACHE_NAME_INFO.put(modelId, info);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static ServerModelInfo cacheModel(ZipFile zipFile, String modelId, boolean isAuth) {
        try {
            ModelData data = getModelData(zipFile, modelId, isAuth);
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
    private static ModelData getModelData(ZipFile zipFile, String modelId, boolean isAuth) throws IOException {
        Map<String, byte[]> model = Maps.newHashMap();
        model.put("main", getBytes(zipFile, MAIN_MODEL_FILE_NAME));
        model.put("arm", getBytes(zipFile, ARM_MODEL_FILE_NAME));

        Map<String, byte[]> texture = Maps.newHashMap();
        zipFile.stream().forEach(zipEntry -> {
            if (zipEntry.getName().endsWith(".png")) {
                try {
                    texture.put(zipEntry.getName(), getBytes(zipFile, zipEntry.getName()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Map<String, byte[]> animation = Maps.newHashMap();
        animation.put("main", getBytes(zipFile, MAIN_ANIMATION_FILE_NAME));
        animation.put("arm", getBytes(zipFile, ARM_ANIMATION_FILE_NAME));
        animation.put("extra", getBytes(zipFile, EXTRA_ANIMATION_FILE_NAME));

        return new ModelData(modelId, isAuth, Type.ZIP, model, texture, animation);
    }

    private static byte[] getBytes(ZipFile zipFile, String fileName) throws IOException {
        if (MAIN_ANIMATION_FILE_NAME.equals(fileName) && zipFile.getEntry(MAIN_ANIMATION_FILE_NAME) == null) {
            Path filePath = CUSTOM.resolve("default/main.animation.json");
            return FileUtils.readFileToByteArray(filePath.toFile());
        }
        if (ARM_ANIMATION_FILE_NAME.equals(fileName) && zipFile.getEntry(ARM_ANIMATION_FILE_NAME) == null) {
            Path filePath = CUSTOM.resolve("default/arm.animation.json");
            return FileUtils.readFileToByteArray(filePath.toFile());
        }
        if (EXTRA_ANIMATION_FILE_NAME.equals(fileName) && zipFile.getEntry(EXTRA_ANIMATION_FILE_NAME) == null) {
            Path filePath = CUSTOM.resolve("default/extra.animation.json");
            return FileUtils.readFileToByteArray(filePath.toFile());
        }

        ZipEntry entry = zipFile.getEntry(fileName);
        try (InputStream stream = zipFile.getInputStream(entry)) {
            byte[] bytes = InputStreamUtils.toBytes(stream);
            if (MAIN_MODEL_FILE_NAME.equals(fileName) || ARM_MODEL_FILE_NAME.equals(fileName)) {
                String modelJson = new String(bytes, StandardCharsets.UTF_8);
                RawGeoModel rawModel = Converter.fromJsonString(modelJson);
                return ObjectStreamUtil.toByteArray(rawModel);
            }
            return bytes;
        }
    }

    private static boolean isBlankEntry(ZipFile zipFile, String fileName) {
        ZipEntry entry = zipFile.getEntry(fileName);
        try (InputStream stream = zipFile.getInputStream(entry)) {
            String fileText = IOUtils.toString(stream, StandardCharsets.UTF_8);
            return StringUtils.isBlank(fileText);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}
