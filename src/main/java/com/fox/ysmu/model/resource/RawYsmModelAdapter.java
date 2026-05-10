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
    private static final String ANIMATION_FORMAT_VERSION = "1.8.0";
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
            if (!animations.containsKey(entry.getKey())) {
                putAnimationFile(animations, entry.getKey(), entry.getValue());
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
        JsonObject polyMesh = createPolyMeshJson(rawBone);
        if (polyMesh != null) {
            bone.add("poly_mesh", polyMesh);
        }
        return bone;
    }

    private static JsonObject createPolyMeshJson(RawYsmModel.RawBone rawBone) {
        JsonArray positions = new JsonArray();
        JsonArray normals = new JsonArray();
        JsonArray uvs = new JsonArray();

        for (RawYsmModel.RawCube rawCube : rawBone.cubes) {
            for (RawYsmModel.RawFace face : rawCube.faces) {
                for (int i = 0; i < 4; i++) {
                    float[] position = face.positions[i];
                    positions.add(new JsonPrimitive((double) getVectorValue(position, 0)));
                    positions.add(new JsonPrimitive((double) getVectorValue(position, 1)));
                    positions.add(new JsonPrimitive((double) getVectorValue(position, 2)));

                    normals.add(new JsonPrimitive((double) getVectorValue(face.normal, 0)));
                    normals.add(new JsonPrimitive((double) getVectorValue(face.normal, 1)));
                    normals.add(new JsonPrimitive((double) getVectorValue(face.normal, 2)));

                    uvs.add(new JsonPrimitive((double) face.u[i]));
                    uvs.add(new JsonPrimitive((double) face.v[i]));
                }
            }
        }

        if (positions.size() == 0) {
            return null;
        }

        JsonObject mesh = new JsonObject();
        mesh.addProperty("normalized_uvs", true);
        mesh.addProperty("polys", "quad_list");
        mesh.add("positions", positions);
        mesh.add("normals", normals);
        mesh.add("uvs", uvs);
        return mesh;
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
        if (putAnimationFile(animations, key, raw.mainEntity.animationFiles.get(key))) {
            return;
        }
        animations.put(key, readDefaultAnimation(defaultFileName));
    }

    private static boolean putAnimationFile(Map<String, byte[]> animations, String key,
        RawYsmModel.RawAnimationFile animationFile) {
        if (animationFile == null) {
            return false;
        }
        if (animationFile.sourceJson != null) {
            animations.put(key, animationFile.sourceJson);
            return true;
        }
        if (!animationFile.animations.isEmpty()) {
            animations.put(key, createAnimationJson(animationFile));
            return true;
        }
        return false;
    }

    private static byte[] createAnimationJson(RawYsmModel.RawAnimationFile animationFile) {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", ANIMATION_FORMAT_VERSION);
        JsonObject animations = new JsonObject();
        for (RawYsmModel.RawAnimation animation : animationFile.animations.values()) {
            if (StringUtils.isBlank(animation.name)) {
                continue;
            }
            animations.add(animation.name, createAnimationJson(animation));
        }
        root.add("animations", animations);
        return ysmu.GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    private static JsonObject createAnimationJson(RawYsmModel.RawAnimation animation) {
        JsonObject json = new JsonObject();
        if (isFinite(animation.length) && animation.length > 0f) {
            json.addProperty("animation_length", (double) animation.length);
        }
        putLoopMode(json, animation.loopMode);
        if (animation.blendWeight != null) {
            json.add("blend_weight", molangValue(animation.blendWeight));
        }

        JsonObject bones = new JsonObject();
        for (RawYsmModel.RawBoneAnimation boneAnimation : animation.boneAnimations) {
            if (StringUtils.isBlank(boneAnimation.boneName)) {
                continue;
            }
            JsonObject bone = new JsonObject();
            putChannel(bone, "rotation", boneAnimation.rotation);
            putChannel(bone, "position", boneAnimation.position);
            putChannel(bone, "scale", boneAnimation.scale);
            if (!bone.entrySet().isEmpty()) {
                bones.add(boneAnimation.boneName, bone);
            }
        }
        if (!bones.entrySet().isEmpty()) {
            json.add("bones", bones);
        }

        putTimeline(json, animation.timelineEvents);
        putSoundEffects(json, animation.soundEffects);
        return json;
    }

    private static void putLoopMode(JsonObject json, int loopMode) {
        if (loopMode == 1) {
            json.addProperty("loop", true);
        } else if (loopMode == 3) {
            json.addProperty("loop", "hold_on_last_frame");
        } else if (loopMode == 0) {
            json.addProperty("loop", false);
        }
    }

    private static void putChannel(JsonObject bone, String channelName, List<RawYsmModel.RawKeyframe> keyframes) {
        if (keyframes.isEmpty()) {
            return;
        }
        JsonObject channel = new JsonObject();
        for (RawYsmModel.RawKeyframe keyframe : keyframes) {
            channel.add(timeKey(keyframe.timestamp), createKeyframeJson(keyframe));
        }
        bone.add(channelName, channel);
    }

    private static JsonElement createKeyframeJson(RawYsmModel.RawKeyframe keyframe) {
        String lerpMode = lerpMode(keyframe.interpolationMode);
        if (!keyframe.hasPreData) {
            JsonArray value = molangArray(keyframe.postData);
            if (StringUtils.isBlank(lerpMode)) {
                return value;
            }
            JsonObject json = new JsonObject();
            json.add("vector", value);
            json.addProperty("lerp_mode", lerpMode);
            return json;
        }
        JsonObject json = new JsonObject();
        json.add("pre", molangArray(keyframe.preData));
        json.add("post", molangArray(keyframe.postData));
        if (StringUtils.isNotBlank(lerpMode)) {
            json.addProperty("lerp_mode", lerpMode);
        }
        return json;
    }

    private static String lerpMode(int interpolationMode) {
        if (interpolationMode == 1) {
            return "step";
        }
        if (interpolationMode == 2) {
            return "catmullrom";
        }
        return "";
    }

    private static JsonArray molangArray(Object[] values) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < 3; i++) {
            array.add(molangValue(values != null && values.length > i ? values[i] : Float.valueOf(0f)));
        }
        return array;
    }

    private static JsonPrimitive molangValue(Object value) {
        if (value instanceof Number) {
            return new JsonPrimitive((Number) value);
        }
        if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        }
        return new JsonPrimitive(value == null ? "0" : value.toString());
    }

    private static void putTimeline(JsonObject animationJson, List<RawYsmModel.RawTimelineEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        JsonObject timeline = new JsonObject();
        for (RawYsmModel.RawTimelineEvent event : events) {
            if (event.events.isEmpty()) {
                continue;
            }
            String key = timeKey(event.timestamp);
            if (event.events.size() == 1 && !timeline.has(key)) {
                timeline.addProperty(key, event.events.get(0));
            } else {
                JsonArray array = timeline.has(key) && timeline.get(key).isJsonArray()
                    ? timeline.getAsJsonArray(key)
                    : new JsonArray();
                if (timeline.has(key) && timeline.get(key).isJsonPrimitive()) {
                    array.add(new JsonPrimitive(timeline.get(key).getAsString()));
                }
                for (String instruction : event.events) {
                    array.add(new JsonPrimitive(instruction));
                }
                timeline.add(key, array);
            }
        }
        if (!timeline.entrySet().isEmpty()) {
            animationJson.add("timeline", timeline);
        }
    }

    private static void putSoundEffects(JsonObject animationJson, List<RawYsmModel.RawSoundEffect> effects) {
        if (effects.isEmpty()) {
            return;
        }
        JsonObject soundEffects = new JsonObject();
        for (RawYsmModel.RawSoundEffect effect : effects) {
            if (StringUtils.isBlank(effect.effectName)) {
                continue;
            }
            JsonObject frame = new JsonObject();
            frame.addProperty("effect", effect.effectName);
            soundEffects.add(timeKey(effect.timestamp), frame);
        }
        if (!soundEffects.entrySet().isEmpty()) {
            animationJson.add("sound_effects", soundEffects);
        }
    }

    private static String timeKey(float seconds) {
        return isFinite(seconds) ? Float.toString(seconds) : "0.0";
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
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

}
