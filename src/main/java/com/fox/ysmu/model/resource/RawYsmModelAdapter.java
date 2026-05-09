package com.fox.ysmu.model.resource;

import static com.fox.ysmu.model.ServerModelManager.ARM_ANIMATION_FILE_NAME;
import static com.fox.ysmu.model.ServerModelManager.CUSTOM;
import static com.fox.ysmu.model.ServerModelManager.EXTRA_ANIMATION_FILE_NAME;
import static com.fox.ysmu.model.ServerModelManager.MAIN_ANIMATION_FILE_NAME;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.model.format.Type;
import com.fox.ysmu.model.resource.pojo.RawYsmModel;

public final class RawYsmModelAdapter {

    private static final byte[] EMPTY_ANIMATION = "{\"animations\":{}}".getBytes(StandardCharsets.UTF_8);

    private RawYsmModelAdapter() {}

    public static boolean isBridgeable(RawYsmModel raw) {
        if (raw == null || raw.mainEntity == null) {
            return false;
        }
        if (raw.mainEntity.mainModel == null || raw.mainEntity.mainModel.sourceJson == null) {
            return false;
        }
        if (raw.mainEntity.armModel == null || raw.mainEntity.armModel.sourceJson == null) {
            return false;
        }
        for (RawYsmModel.RawTexture texture : raw.mainEntity.textures.values()) {
            if (texture.imageFormat == 2 && texture.data != null) {
                return true;
            }
        }
        return false;
    }

    public static ModelData toLegacyModelData(RawYsmModel raw, String modelId) throws IOException {
        if (!isBridgeable(raw)) {
            throw new IOException("RawYsmModel cannot be bridged to legacy ModelData");
        }

        Map<String, byte[]> model = new LinkedHashMap<>();
        model.put("main", raw.mainEntity.mainModel.sourceJson);
        model.put("arm", raw.mainEntity.armModel.sourceJson);

        Map<String, byte[]> textures = new LinkedHashMap<>();
        for (RawYsmModel.RawTexture texture : raw.mainEntity.textures.values()) {
            if (texture.imageFormat != 2 || texture.data == null) {
                continue;
            }
            String fileName = texture.sourceFileName;
            if (StringUtils.isBlank(fileName)) {
                fileName = texture.name.endsWith(".png") ? texture.name : texture.name + ".png";
            }
            textures.put(fileName, texture.data);
        }
        if (textures.isEmpty()) {
            throw new IOException("RawYsmModel has no PNG player textures for legacy bridge");
        }

        Map<String, byte[]> animations = new LinkedHashMap<>();
        putAnimation(animations, raw, "main", MAIN_ANIMATION_FILE_NAME);
        putAnimation(animations, raw, "arm", ARM_ANIMATION_FILE_NAME);
        putAnimation(animations, raw, "extra", EXTRA_ANIMATION_FILE_NAME);
        for (Map.Entry<String, RawYsmModel.RawAnimationFile> entry : raw.mainEntity.animationFiles.entrySet()) {
            if (!animations.containsKey(entry.getKey()) && entry.getValue().sourceJson != null) {
                animations.put(entry.getKey(), entry.getValue().sourceJson);
            }
        }

        return new ModelData(modelId, Type.FOLDER, model, textures, animations);
    }

    private static void putAnimation(Map<String, byte[]> animations, RawYsmModel raw, String key, String defaultFileName)
        throws IOException {
        RawYsmModel.RawAnimationFile animationFile = raw.mainEntity.animationFiles.get(key);
        if (animationFile != null && animationFile.sourceJson != null) {
            animations.put(key, animationFile.sourceJson);
            return;
        }
        animations.put(key, readDefaultAnimation(defaultFileName));
    }

    private static byte[] readDefaultAnimation(String fileName) throws IOException {
        Path defaultPath = CUSTOM.resolve("default").resolve(fileName);
        if (Files.isRegularFile(defaultPath)) {
            return Files.readAllBytes(defaultPath);
        }
        return EMPTY_ANIMATION;
    }
}
