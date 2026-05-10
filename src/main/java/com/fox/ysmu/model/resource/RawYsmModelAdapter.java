package com.fox.ysmu.model.resource;

import static com.fox.ysmu.model.ServerModelManager.ARM_ANIMATION_FILE_NAME;
import static com.fox.ysmu.model.ServerModelManager.CUSTOM;
import static com.fox.ysmu.model.ServerModelManager.EXTRA_ANIMATION_FILE_NAME;
import static com.fox.ysmu.model.ServerModelManager.MAIN_ANIMATION_FILE_NAME;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;

import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.model.format.Type;
import com.fox.ysmu.model.resource.pojo.RawYsmModel;
import com.fox.ysmu.ysmu;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public final class RawYsmModelAdapter {

    private static final byte[] EMPTY_ANIMATION = "{\"animations\":{}}".getBytes(StandardCharsets.UTF_8);
    private static final int RGBA_FORMAT = -1;
    private static final int PNG_FORMAT = 2;
    private static final int EXTRA_ANIMATION_SLOT_COUNT = 8;
    private static final String[] LOCALE_PREFERENCE = new String[] { "en_us", "en_US", "zh_cn", "zh_CN" };

    private RawYsmModelAdapter() {}

    public static boolean isBridgeable(RawYsmModel raw) {
        if (raw == null || raw.mainEntity == null) {
            return false;
        }
        if (!hasGeometry(raw.mainEntity.mainModel)) {
            return false;
        }
        if (!hasGeometry(raw.mainEntity.armModel)) {
            return false;
        }
        for (RawYsmModel.RawTexture texture : raw.mainEntity.textures.values()) {
            if (hasLegacyTextureData(texture)) {
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
        model.put("main", toGeometryJson(raw, raw.mainEntity.mainModel, true));
        model.put("arm", toGeometryJson(raw, raw.mainEntity.armModel, false));

        Map<String, byte[]> textures = new LinkedHashMap<>();
        for (RawYsmModel.RawTexture texture : raw.mainEntity.textures.values()) {
            if (texture.data == null) {
                continue;
            }
            byte[] textureData = getLegacyTextureData(texture);
            if (textureData == null) {
                ysmu.LOG.warn(
                    "Skipping unsupported OpenYSM texture {} (format {}) in model {}",
                    textureName(texture),
                    texture.imageFormat,
                    modelId);
                continue;
            }
            String fileName = texture.sourceFileName;
            if (StringUtils.isBlank(fileName)) {
                fileName = texture.name.endsWith(".png") ? texture.name : texture.name + ".png";
            }
            textures.put(fileName, textureData);
        }
        if (textures.isEmpty()) {
            throw new IOException("RawYsmModel has no legacy-compatible player textures");
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
        putAnimationControllers(animations, raw);

        return new ModelData(modelId, Type.FOLDER, model, textures, animations);
    }

    private static boolean hasGeometry(RawYsmModel.RawGeometry geometry) {
        if (geometry == null) {
            return false;
        }
        if (geometry.sourceJson != null) {
            return true;
        }
        for (RawYsmModel.RawBone bone : geometry.bones) {
            for (RawYsmModel.RawCube cube : bone.cubes) {
                if (!cube.faces.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasLegacyTextureData(RawYsmModel.RawTexture texture) {
        if (texture == null || texture.data == null) {
            return false;
        }
        if (texture.imageFormat == PNG_FORMAT) {
            return true;
        }
        return texture.imageFormat == RGBA_FORMAT && canConvertRgba(texture);
    }

    private static byte[] getLegacyTextureData(RawYsmModel.RawTexture texture) {
        if (texture.imageFormat == PNG_FORMAT) {
            return texture.data;
        }
        if (texture.imageFormat == RGBA_FORMAT && canConvertRgba(texture)) {
            return convertRgbaToPng(texture.data, texture.width, texture.height);
        }
        return null;
    }

    private static boolean canConvertRgba(RawYsmModel.RawTexture texture) {
        long requiredBytes = (long) texture.width * texture.height * 4L;
        return texture.width > 0 && texture.height > 0 && texture.data.length >= requiredBytes;
    }

    private static byte[] convertRgbaToPng(byte[] rgbaData, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            int r = rgbaData[i * 4] & 0xFF;
            int g = rgbaData[i * 4 + 1] & 0xFF;
            int b = rgbaData[i * 4 + 2] & 0xFF;
            int a = rgbaData[i * 4 + 3] & 0xFF;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (ImageIO.write(image, "PNG", output)) {
                return output.toByteArray();
            }
        } catch (IOException e) {
            ysmu.LOG.warn("Failed to convert OpenYSM RGBA texture to PNG", e);
        }
        return null;
    }

    private static byte[] toGeometryJson(RawYsmModel raw, RawYsmModel.RawGeometry geometry, boolean includeModelInfo)
        throws IOException {
        JsonObject root;
        if (geometry.sourceJson != null) {
            root = parseJsonObject(geometry.sourceJson, geometry.identifier);
        } else {
            root = createGeometryJson(geometry);
        }

        JsonObject description = getOrCreateDescription(root);
        if (includeModelInfo) {
            applyOpenYsmModelInfo(raw, description);
        }
        return ysmu.GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    private static JsonObject parseJsonObject(byte[] bytes, String sourceName) throws IOException {
        JsonElement element;
        try {
            element = new JsonParser().parse(new String(bytes, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            throw new IOException("Invalid geometry JSON " + sourceName, e);
        }
        if (element == null || !element.isJsonObject()) {
            throw new IOException("Expected geometry JSON object " + sourceName);
        }
        return element.getAsJsonObject();
    }

    private static JsonObject createGeometryJson(RawYsmModel.RawGeometry geometry) {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.12.0");
        JsonArray geometries = new JsonArray();
        JsonObject model = new JsonObject();
        JsonObject description = new JsonObject();
        description.addProperty(
            "identifier",
            StringUtils.defaultIfBlank(geometry.identifier, "geometry.ysmu.generated"));
        description.addProperty("texture_width", (double) defaultPositive(geometry.textureWidth, 64f));
        description.addProperty("texture_height", (double) defaultPositive(geometry.textureHeight, 64f));
        if (geometry.visibleBoundsWidth > 0f) {
            description.addProperty("visible_bounds_width", (double) geometry.visibleBoundsWidth);
        }
        if (geometry.visibleBoundsHeight > 0f) {
            description.addProperty("visible_bounds_height", (double) geometry.visibleBoundsHeight);
        }
        if (geometry.visibleBoundsOffset != null && geometry.visibleBoundsOffset.length > 0) {
            description.add("visible_bounds_offset", floatArray(geometry.visibleBoundsOffset));
        }
        model.add("description", description);

        JsonArray bones = new JsonArray();
        for (RawYsmModel.RawBone rawBone : geometry.bones) {
            bones.add(createBoneJson(rawBone, geometry));
        }
        model.add("bones", bones);
        geometries.add(model);
        root.add("minecraft:geometry", geometries);
        return root;
    }

    private static JsonObject createBoneJson(RawYsmModel.RawBone rawBone, RawYsmModel.RawGeometry geometry) {
        JsonObject bone = new JsonObject();
        bone.addProperty("name", StringUtils.defaultIfBlank(rawBone.name, "bone"));
        if (StringUtils.isNotBlank(rawBone.parentName)) {
            bone.addProperty("parent", rawBone.parentName);
        }
        bone.add("pivot", generatedPivotArray(rawBone.pivot));
        bone.add("rotation", generatedRotationArray(rawBone.rotation));
        if (!rawBone.cubes.isEmpty()) {
            JsonArray cubes = new JsonArray();
            for (RawYsmModel.RawCube rawCube : rawBone.cubes) {
                JsonObject cube = createCubeJson(rawCube, geometry);
                if (cube != null) {
                    cubes.add(cube);
                }
            }
            if (cubes.size() > 0) {
                bone.add("cubes", cubes);
            }
        }
        return bone;
    }

    private static JsonObject createCubeJson(RawYsmModel.RawCube rawCube, RawYsmModel.RawGeometry geometry) {
        Bounds bounds = getBounds(rawCube);
        if (bounds == null) {
            return null;
        }
        float textureWidth = defaultPositive(geometry.textureWidth, 64f);
        float textureHeight = defaultPositive(geometry.textureHeight, 64f);

        JsonObject cube = new JsonObject();
        cube.add("origin", doubleArray(-bounds.maxX * 16d, bounds.minY * 16d, bounds.minZ * 16d));
        cube.add(
            "size",
            doubleArray(
                (bounds.maxX - bounds.minX) * 16d,
                (bounds.maxY - bounds.minY) * 16d,
                (bounds.maxZ - bounds.minZ) * 16d));
        JsonObject faces = new JsonObject();
        for (RawYsmModel.RawFace face : rawCube.faces) {
            String direction = getFaceDirection(face);
            if (direction != null && !faces.has(direction)) {
                faces.add(direction, createFaceUvJson(face, textureWidth, textureHeight));
            }
        }
        if (faces.entrySet().isEmpty()) {
            return null;
        }
        cube.add("uv", faces);
        return cube;
    }

    private static Bounds getBounds(RawYsmModel.RawCube rawCube) {
        Bounds bounds = new Bounds();
        boolean found = false;
        for (RawYsmModel.RawFace face : rawCube.faces) {
            for (float[] position : face.positions) {
                if (position == null || position.length < 3) {
                    continue;
                }
                bounds.add(position[0], position[1], position[2]);
                found = true;
            }
        }
        return found ? bounds : null;
    }

    private static String getFaceDirection(RawYsmModel.RawFace face) {
        float x = face.normal.length > 0 ? face.normal[0] : 0f;
        float y = face.normal.length > 1 ? face.normal[1] : 0f;
        float z = face.normal.length > 2 ? face.normal[2] : 0f;
        float absX = Math.abs(x);
        float absY = Math.abs(y);
        float absZ = Math.abs(z);
        if (absX >= absY && absX >= absZ && absX > 0f) {
            return x > 0f ? "east" : "west";
        }
        if (absY >= absZ && absY > 0f) {
            return y > 0f ? "up" : "down";
        }
        if (absZ > 0f) {
            return z > 0f ? "south" : "north";
        }
        return null;
    }

    private static JsonObject createFaceUvJson(RawYsmModel.RawFace face, float textureWidth, float textureHeight) {
        float minU = Float.POSITIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            minU = Math.min(minU, face.u[i]);
            minV = Math.min(minV, face.v[i]);
            maxU = Math.max(maxU, face.u[i]);
            maxV = Math.max(maxV, face.v[i]);
        }
        if (!Float.isFinite(minU) || !Float.isFinite(minV) || !Float.isFinite(maxU) || !Float.isFinite(maxV)) {
            minU = 0f;
            minV = 0f;
            maxU = 1f / textureWidth;
            maxV = 1f / textureHeight;
        }
        JsonObject uv = new JsonObject();
        uv.add("uv", doubleArray(minU * textureWidth, minV * textureHeight));
        uv.add("uv_size", doubleArray((maxU - minU) * textureWidth, (maxV - minV) * textureHeight));
        return uv;
    }

    private static JsonObject getOrCreateDescription(JsonObject root) {
        JsonObject geometry = getOrCreateGeometry(root);
        JsonObject description;
        if (geometry.has("description") && geometry.get("description").isJsonObject()) {
            description = geometry.getAsJsonObject("description");
        } else {
            description = new JsonObject();
            geometry.add("description", description);
        }
        return description;
    }

    private static JsonObject getOrCreateGeometry(JsonObject root) {
        JsonArray geometries;
        if (root.has("minecraft:geometry") && root.get("minecraft:geometry").isJsonArray()) {
            geometries = root.getAsJsonArray("minecraft:geometry");
        } else {
            geometries = new JsonArray();
            root.add("minecraft:geometry", geometries);
        }
        if (geometries.size() > 0 && geometries.get(0).isJsonObject()) {
            return geometries.get(0).getAsJsonObject();
        }
        JsonObject geometry = new JsonObject();
        geometries.add(geometry);
        return geometry;
    }

    private static void applyOpenYsmModelInfo(RawYsmModel raw, JsonObject description) {
        description.addProperty("ysm_height_scale", (double) raw.properties.heightScale);
        description.addProperty("ysm_width_scale", (double) raw.properties.widthScale);

        JsonObject extraInfo = new JsonObject();
        extraInfo.addProperty("name", raw.metadata.name);
        extraInfo.addProperty("tips", raw.metadata.tips);
        String[] extraAnimationNames = getExtraAnimationNames(raw);
        if (extraAnimationNames.length > 0) {
            extraInfo.add("extra_animation_names", stringArray(extraAnimationNames));
        }

        List<String> authors = new ArrayList<>();
        for (RawYsmModel.RawMetadata.Author author : raw.metadata.authors) {
            if (StringUtils.isBlank(author.name)) {
                continue;
            }
            if (StringUtils.isBlank(author.role)) {
                authors.add(author.name);
            } else {
                authors.add(author.name + " (" + author.role + ")");
            }
        }
        if (!authors.isEmpty()) {
            extraInfo.add("authors", stringArray(authors.toArray(new String[0])));
        }

        String license = StringUtils.defaultIfBlank(raw.metadata.licenseType, raw.metadata.licenseDescription);
        if (StringUtils.isNotBlank(license)) {
            extraInfo.addProperty("license", license);
        }
        description.add("ysm_extra_info", extraInfo);
    }

    private static String[] getExtraAnimationNames(RawYsmModel raw) {
        if (raw.properties.extraAnimations.isEmpty()) {
            return new String[0];
        }
        String[] names = new String[EXTRA_ANIMATION_SLOT_COUNT];
        boolean hasAny = false;
        for (int i = 0; i < EXTRA_ANIMATION_SLOT_COUNT; i++) {
            String key = "extra" + i;
            if (!raw.properties.extraAnimations.containsKey(key)) {
                continue;
            }
            String label = getLocalizedValue(raw, "properties.extra_animation." + key);
            if (StringUtils.isBlank(label)) {
                String configured = raw.properties.extraAnimations.get(key);
                if (StringUtils.isNotBlank(configured) && !configured.startsWith("#")) {
                    label = configured;
                }
            }
            if (StringUtils.isBlank(label)) {
                label = key;
            }
            names[i] = label;
            hasAny = true;
        }
        return hasAny ? names : new String[0];
    }

    private static String getLocalizedValue(RawYsmModel raw, String key) {
        for (String locale : LOCALE_PREFERENCE) {
            RawYsmModel.RawLanguageFile file = raw.languageFiles.get(locale);
            if (file != null && file.data.containsKey(key)) {
                return file.data.get(key);
            }
        }
        for (RawYsmModel.RawLanguageFile file : raw.languageFiles.values()) {
            if (file.data.containsKey(key)) {
                return file.data.get(key);
            }
        }
        return "";
    }

    private static JsonArray stringArray(String[] values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(new JsonPrimitive(value == null ? "" : value));
        }
        return array;
    }

    private static JsonArray floatArray(float[] values) {
        JsonArray array = new JsonArray();
        if (values == null) {
            return array;
        }
        for (float value : values) {
            array.add(new JsonPrimitive((double) value));
        }
        return array;
    }

    private static JsonArray generatedPivotArray(float[] pivot) {
        float x = getVectorValue(pivot, 0);
        float y = getVectorValue(pivot, 1);
        float z = getVectorValue(pivot, 2);
        return doubleArray(-x, y, z);
    }

    private static JsonArray generatedRotationArray(float[] rotation) {
        float x = getVectorValue(rotation, 0);
        float y = getVectorValue(rotation, 1);
        float z = getVectorValue(rotation, 2);
        return doubleArray(-Math.toDegrees(x), -Math.toDegrees(y), Math.toDegrees(z));
    }

    private static float getVectorValue(float[] values, int index) {
        return values != null && values.length > index ? values[index] : 0f;
    }

    private static JsonArray doubleArray(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) {
            array.add(new JsonPrimitive(value));
        }
        return array;
    }

    private static float defaultPositive(float value, float defaultValue) {
        return value > 0f ? value : defaultValue;
    }

    private static String textureName(RawYsmModel.RawTexture texture) {
        return StringUtils.defaultIfBlank(texture.sourceFileName, texture.name);
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

    private static void putAnimationControllers(Map<String, byte[]> animations, RawYsmModel raw) {
        int index = 0;
        for (RawYsmModel.RawAnimationControllerFile file : raw.mainEntity.animationControllerFiles) {
            if (file == null || (file.sourceJson == null && file.controllers.isEmpty())) {
                index++;
                continue;
            }
            byte[] data = file.sourceJson == null ? createControllerJson(file) : file.sourceJson;
            animations.put(YsmControllerResources.resourceName(file.name, index), data);
            index++;
        }
    }

    private static byte[] createControllerJson(RawYsmModel.RawAnimationControllerFile file) {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.19.0");
        JsonObject controllers = new JsonObject();
        for (RawYsmModel.RawAnimationController controller : file.controllers.values()) {
            controllers.add(controller.animationName, createControllerJson(controller));
        }
        root.add("animation_controllers", controllers);
        return ysmu.GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    private static JsonObject createControllerJson(RawYsmModel.RawAnimationController controller) {
        JsonObject json = new JsonObject();
        if (StringUtils.isNotBlank(controller.initialState)) {
            json.addProperty("initial_state", controller.initialState);
        }
        JsonObject states = new JsonObject();
        for (RawYsmModel.RawControllerState state : controller.states) {
            states.add(state.name, createStateJson(state));
        }
        json.add("states", states);
        return json;
    }

    private static JsonObject createStateJson(RawYsmModel.RawControllerState state) {
        JsonObject json = new JsonObject();
        JsonArray animations = new JsonArray();
        for (Map.Entry<String, String> entry : state.animations.entrySet()) {
            if (StringUtils.isBlank(entry.getValue())) {
                animations.add(new JsonPrimitive(entry.getKey()));
            } else {
                JsonObject conditional = new JsonObject();
                conditional.addProperty(entry.getKey(), entry.getValue());
                animations.add(conditional);
            }
        }
        if (animations.size() > 0) {
            json.add("animations", animations);
        }

        JsonArray transitions = new JsonArray();
        for (Map.Entry<String, String> entry : state.transitions.entrySet()) {
            JsonObject transition = new JsonObject();
            transition.addProperty(entry.getKey(), entry.getValue());
            transitions.add(transition);
        }
        if (transitions.size() > 0) {
            json.add("transitions", transitions);
        }
        putStringArray(json, "on_entry", state.onEntry);
        putStringArray(json, "on_exit", state.onExit);
        putStringArray(json, "sound_effects", state.soundEffects);
        if (!state.blendTransitions.isEmpty()) {
            JsonObject blendTransitions = new JsonObject();
            for (Map.Entry<Float, Float> entry : state.blendTransitions.entrySet()) {
                blendTransitions.addProperty(Float.toString(entry.getKey()), entry.getValue());
            }
            json.add("blend_transition", blendTransitions);
        } else if (state.blendTransitionValue > 0f) {
            json.addProperty("blend_transition", state.blendTransitionValue);
        }
        if (state.blendViaShortestPath) {
            json.addProperty("blend_via_shortest_path", true);
        }
        return json;
    }

    private static void putStringArray(JsonObject json, String name, List<String> values) {
        if (values.isEmpty()) {
            return;
        }
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(new JsonPrimitive(value));
        }
        json.add(name, array);
    }

    private static final class Bounds {

        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        private void add(float x, float y, float z) {
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
        }
    }
}
