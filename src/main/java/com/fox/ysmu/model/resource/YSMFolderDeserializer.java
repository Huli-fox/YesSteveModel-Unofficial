package com.fox.ysmu.model.resource;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import com.fox.ysmu.model.resource.pojo.RawYsmModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import software.bernie.geckolib3.geo.raw.pojo.Converter;

public class YSMFolderDeserializer implements AutoCloseable {

    private static final String YSM_JSON = "ysm.json";
    private static final String MAIN_JSON = "main.json";
    private static final String ARM_JSON = "arm.json";
    private static final String MAIN_ANIMATION_JSON = "main.animation.json";
    private static final String ARM_ANIMATION_JSON = "arm.animation.json";
    private static final String EXTRA_ANIMATION_JSON = "extra.animation.json";

    private final Map<String, String> readFilesMd5Map = new TreeMap<>();
    private final Path rootPath;
    private final RawYsmModel model = new RawYsmModel();
    private String finalFolderHash = "";

    public YSMFolderDeserializer(Path sourcePath) throws IOException {
        if (sourcePath == null || !Files.exists(sourcePath)) {
            throw new FileNotFoundException("Model source not found: " + sourcePath);
        }
        if (!Files.isDirectory(sourcePath)) {
            throw new IllegalArgumentException("Expected an OpenYSM model directory: " + sourcePath);
        }
        this.rootPath = sourcePath.toAbsolutePath().normalize();
        this.model.formatVersion = 65535;
        this.model.modelId = sourcePath.getFileName() == null ? "" : sourcePath.getFileName().toString();
    }

    public RawYsmModel deserialize() throws IOException {
        byte[] ysmJsonBytes = readResource(YSM_JSON);
        if (ysmJsonBytes != null) {
            JsonObject ysmJson = parseObject(ysmJsonBytes, YSM_JSON);
            parseYsmJson(ysmJson);
        } else {
            parseLegacyFormat();
        }

        parseGlobalResources();
        this.finalFolderHash = calculateFinalFolderHash();
        this.model.properties.sha256 = this.finalFolderHash;
        this.model.footer.version = 65535;
        validateMainPlayerModel();
        return this.model;
    }

    @Override
    public void close() {}

    public String getFolderHash() {
        return this.finalFolderHash;
    }

    private byte[] readResource(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        String normalizedRelative = normalizeResourcePath(relativePath);
        Path target = this.rootPath.resolve(normalizedRelative).normalize();
        if (!target.startsWith(this.rootPath) || !Files.isRegularFile(target)) {
            return null;
        }
        byte[] data = Files.readAllBytes(target);
        if (!this.readFilesMd5Map.containsKey(normalizedRelative)) {
            this.readFilesMd5Map.put(normalizedRelative, md5Hex(data));
        }
        return data;
    }

    private static String normalizeResourcePath(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private void parseYsmJson(JsonObject ysmJson) throws IOException {
        if (hasObject(ysmJson, "metadata")) {
            parseMetadata(ysmJson.getAsJsonObject("metadata"));
        }
        if (hasObject(ysmJson, "properties")) {
            parseProperties(ysmJson.getAsJsonObject("properties"));
        }
        if (!hasObject(ysmJson, "files")) {
            throw new IOException("OpenYSM model is missing files section");
        }

        JsonObject files = ysmJson.getAsJsonObject("files");
        if (hasObject(files, "player")) {
            parseMainEntity(files.getAsJsonObject("player"));
        }
        if (files.has("vehicles")) {
            parseSubEntities(files.get("vehicles"), this.model.vehicles, "vehicle");
        }
        if (files.has("projectiles")) {
            parseSubEntities(files.get("projectiles"), this.model.projectiles, "projectile");
        }
    }

    private void parseMetadata(JsonObject metaObj) throws IOException {
        this.model.metadata.name = getStr(metaObj, "name", "");
        this.model.metadata.tips = getStr(metaObj, "tips", "");
        if (hasObject(metaObj, "license")) {
            JsonObject license = metaObj.getAsJsonObject("license");
            this.model.metadata.licenseType = getStr(license, "type", "");
            this.model.metadata.licenseDescription = getStr(license, "desc", "");
        }

        if (hasArray(metaObj, "authors")) {
            for (JsonElement elem : metaObj.getAsJsonArray("authors")) {
                if (!elem.isJsonObject()) {
                    continue;
                }
                JsonObject authorObj = elem.getAsJsonObject();
                RawYsmModel.RawMetadata.Author author = new RawYsmModel.RawMetadata.Author();
                author.name = getStr(authorObj, "name", "");
                author.role = getStr(authorObj, "role", "");
                author.comment = getStr(authorObj, "comment", "");
                if (hasObject(authorObj, "contact")) {
                    copyStringMap(authorObj.getAsJsonObject("contact"), author.contacts);
                }
                author.avatar = getStr(authorObj, "avatar", "");
                if (!author.avatar.isEmpty()) {
                    byte[] avatarData = readResource(author.avatar);
                    if (avatarData != null) {
                        author.avatarImage = parseImage(author.name, avatarData);
                    }
                }
                this.model.metadata.authors.add(author);
            }
        }

        if (hasObject(metaObj, "link")) {
            copyStringMap(metaObj.getAsJsonObject("link"), this.model.metadata.links);
        }
    }

    private void parseProperties(JsonObject propsObj) throws IOException {
        this.model.properties.widthScale = (float) getDouble(propsObj, "width_scale", 0.7d);
        this.model.properties.heightScale = (float) getDouble(propsObj, "height_scale", 0.7d);
        this.model.properties.defaultTexture = getStr(propsObj, "default_texture", "default");
        this.model.properties.previewAnimation = getStr(propsObj, "preview_animation", "");
        this.model.properties.isFree = getBool(propsObj, "free", false);
        this.model.properties.renderLayersFirst = getBool(propsObj, "render_layers_first", false);
        this.model.properties.allCutout = getBool(propsObj, "all_cutout", false);
        this.model.properties.disablePreviewRotation = getBool(propsObj, "disable_preview_rotation", false);
        this.model.properties.guiNoLighting = getBool(propsObj, "gui_no_lighting", false);
        this.model.properties.mergeMultilineExpr = getBool(propsObj, "merge_multiline_expr", true);
        this.model.properties.guiForeground = getStr(propsObj, "gui_foreground", "");
        this.model.properties.guiBackground = getStr(propsObj, "gui_background", "");

        if (hasObject(propsObj, "extra_animation")) {
            copyStringMap(propsObj.getAsJsonObject("extra_animation"), this.model.properties.extraAnimations);
        }
        if (hasArray(propsObj, "extra_animation_classify")) {
            for (JsonElement elem : propsObj.getAsJsonArray("extra_animation_classify")) {
                if (!elem.isJsonObject()) {
                    continue;
                }
                JsonObject classifyObj = elem.getAsJsonObject();
                RawYsmModel.ExtraAnimationClassify classify = new RawYsmModel.ExtraAnimationClassify();
                classify.id = getStr(classifyObj, "id", "");
                if (hasObject(classifyObj, "extra_animation")) {
                    copyStringMap(classifyObj.getAsJsonObject("extra_animation"), classify.extras);
                }
                this.model.properties.extraAnimationClassifies.add(classify);
            }
        }
        if (hasArray(propsObj, "extra_animation_buttons")) {
            parseExtraAnimationButtons(propsObj.getAsJsonArray("extra_animation_buttons"));
        }

        loadGuiImage(this.model.properties.guiForeground, "gui_foreground");
        loadGuiImage(this.model.properties.guiBackground, "gui_background");
    }

    private void parseExtraAnimationButtons(JsonArray buttons) {
        for (JsonElement elem : buttons) {
            if (!elem.isJsonObject()) {
                continue;
            }
            JsonObject buttonObj = elem.getAsJsonObject();
            RawYsmModel.ExtraAnimationButton button = new RawYsmModel.ExtraAnimationButton();
            button.id = getStr(buttonObj, "id", "");
            button.name = getStr(buttonObj, "name", "");
            button.description = getStr(buttonObj, "description", "");
            if (hasArray(buttonObj, "config_forms")) {
                for (JsonElement formElem : buttonObj.getAsJsonArray("config_forms")) {
                    if (!formElem.isJsonObject()) {
                        continue;
                    }
                    JsonObject formObj = formElem.getAsJsonObject();
                    RawYsmModel.ConfigForm form = new RawYsmModel.ConfigForm();
                    form.type = getStr(formObj, "type", "");
                    form.title = getStr(formObj, "title", "");
                    form.description = getStr(formObj, "description", "");
                    form.defaultValue = getStr(formObj, "value", "");
                    form.step = (float) getDouble(formObj, "step", 0d);
                    form.min = (float) getDouble(formObj, "min", 0d);
                    form.max = (float) getDouble(formObj, "max", 0d);
                    if (hasObject(formObj, "labels")) {
                        copyStringMap(formObj.getAsJsonObject("labels"), form.labels);
                    }
                    button.forms.add(form);
                }
            }
            this.model.properties.extraAnimationButtons.add(button);
        }
    }

    private void loadGuiImage(String path, String id) throws IOException {
        if (path == null || path.isEmpty()) {
            return;
        }
        byte[] data = readResource(path);
        if (data == null) {
            data = readResource("background/" + id + ".png");
        }
        if (data != null) {
            this.model.properties.backgroundImages.add(parseImage(id, data));
        }
    }

    private void parseMainEntity(JsonObject playerObj) throws IOException {
        if (!hasObject(playerObj, "model")) {
            throw new IOException("OpenYSM player section is missing model object");
        }
        JsonObject modelObj = playerObj.getAsJsonObject("model");
        if (modelObj.has("main")) {
            byte[] data = readResource(modelObj.get("main").getAsString());
            if (data != null) {
                this.model.mainEntity.mainModel = parseGeometry(data, 1, modelObj.get("main").getAsString());
            }
        }
        if (modelObj.has("arm")) {
            byte[] data = readResource(modelObj.get("arm").getAsString());
            if (data != null) {
                this.model.mainEntity.armModel = parseGeometry(data, 2, modelObj.get("arm").getAsString());
            }
        }

        if (playerObj.has("texture")) {
            Iterable<JsonElement> textureElements = asIterable(playerObj.get("texture"));
            for (JsonElement elem : textureElements) {
                parseTextureReference(elem, this.model.mainEntity.textures);
            }
        }

        if (hasObject(playerObj, "animation")) {
            JsonObject animObj = playerObj.getAsJsonObject("animation");
            for (Map.Entry<String, JsonElement> entry : animObj.entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) {
                    continue;
                }
                byte[] animData = readResource(entry.getValue().getAsString());
                if (animData != null) {
                    RawYsmModel.RawAnimationFile animationFile = parseAnimations(animData);
                    animationFile.sourceJson = animData;
                    animationFile.fileHash = sha256Hex(animData);
                    animationFile.animType = getAnimTypeFromKey(entry.getKey());
                    this.model.mainEntity.animationFiles.put(entry.getKey(), animationFile);
                }
            }
        }

        if (hasArray(playerObj, "animation_controllers")) {
            for (JsonElement elem : playerObj.getAsJsonArray("animation_controllers")) {
                if (!elem.isJsonPrimitive()) {
                    continue;
                }
                String path = elem.getAsString();
                byte[] data = readResource(path);
                if (data != null) {
                    RawYsmModel.RawAnimationControllerFile file = new RawYsmModel.RawAnimationControllerFile();
                    file.name = extractFileName(path);
                    file.hash = sha256Hex(data);
                    file.sourceJson = data;
                    parseAnimationControllers(data, file.controllers);
                    this.model.mainEntity.animationControllerFiles.add(file);
                }
            }
        }
    }

    private void parseTextureReference(JsonElement elem, Map<String, RawYsmModel.RawTexture> textures)
        throws IOException {
        String texturePath = null;
        if (elem.isJsonPrimitive()) {
            texturePath = elem.getAsString();
        } else if (elem.isJsonObject() && elem.getAsJsonObject().has("uv")) {
            texturePath = elem.getAsJsonObject().get("uv").getAsString();
        }
        if (texturePath == null || texturePath.isEmpty()) {
            return;
        }
        byte[] textureData = readResource(texturePath);
        if (textureData == null) {
            return;
        }

        RawYsmModel.RawTexture texture = parseTexture(texturePath, textureData);
        if (elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();
            if (obj.has("normal")) {
                addSubTexture(texture, obj.get("normal").getAsString(), 1);
            }
            if (obj.has("specular")) {
                addSubTexture(texture, obj.get("specular").getAsString(), 2);
            }
        }
        textures.put(texture.name, texture);
    }

    private void addSubTexture(RawYsmModel.RawTexture texture, String path, int specularType) throws IOException {
        byte[] data = readResource(path);
        if (data == null) {
            return;
        }
        ImageMeta meta = parseImageMeta(data);
        RawYsmModel.RawTexture.SubTexture subTexture = new RawYsmModel.RawTexture.SubTexture();
        subTexture.specularType = specularType;
        subTexture.hash = sha256Hex(data);
        subTexture.width = meta.width;
        subTexture.height = meta.height;
        subTexture.imageFormat = meta.format;
        subTexture.data = data;
        subTexture.unknownFlag = 1;
        texture.subTextures.add(subTexture);
    }

    private RawYsmModel.RawTexture parseTexture(String path, byte[] data) throws IOException {
        ImageMeta meta = parseImageMeta(data);
        RawYsmModel.RawTexture texture = new RawYsmModel.RawTexture();
        texture.name = extractFileName(path);
        texture.sourceFileName = extractFileNameWithExtension(path);
        texture.hash = sha256Hex(data);
        texture.width = meta.width;
        texture.height = meta.height;
        texture.imageFormat = meta.format;
        texture.data = data;
        texture.unknownFlag = 1;
        return texture;
    }

    private void parseSubEntities(JsonElement sectionElem, Map<String, RawYsmModel.RawSubEntity> targetMap,
        String defaultIdentifier) throws IOException {
        if (!sectionElem.isJsonArray() && !sectionElem.isJsonObject()) {
            return;
        }
        List<JsonObject> items = new ArrayList<>();
        if (sectionElem.isJsonArray()) {
            for (JsonElement elem : sectionElem.getAsJsonArray()) {
                if (elem.isJsonObject()) {
                    items.add(elem.getAsJsonObject());
                }
            }
        } else {
            JsonObject object = sectionElem.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    JsonObject item = entry.getValue().getAsJsonObject();
                    if (!item.has("match")) {
                        item.addProperty("__temp_identifier", entry.getKey());
                    }
                    items.add(item);
                }
            }
        }

        int index = 0;
        for (JsonObject item : items) {
            RawYsmModel.RawSubEntity sub = new RawYsmModel.RawSubEntity();
            sub.identifier = getStr(item, "__temp_identifier", defaultIdentifier + "_" + index);
            if (item.has("match")) {
                sub.matchIds = readStringArray(item.get("match"));
            }
            if (item.has("model")) {
                byte[] modelData = readResource(item.get("model").getAsString());
                if (modelData != null) {
                    sub.model = parseGeometry(modelData, 0, item.get("model").getAsString());
                }
            }
            if (item.has("texture")) {
                for (JsonElement textureElem : asIterable(item.get("texture"))) {
                    parseTextureReference(textureElem, sub.textures);
                }
            }
            if (item.has("animation")) {
                for (JsonElement animElem : asIterable(item.get("animation"))) {
                    if (!animElem.isJsonPrimitive()) {
                        continue;
                    }
                    byte[] animData = readResource(animElem.getAsString());
                    if (animData != null) {
                        RawYsmModel.RawAnimationFile animationFile = parseAnimations(animData);
                        animationFile.sourceJson = animData;
                        animationFile.fileHash = sha256Hex(animData);
                        sub.animationFiles.put(extractFileName(animElem.getAsString()), animationFile);
                    }
                }
            }
            targetMap.put(sub.identifier, sub);
            index++;
        }
    }

    private RawYsmModel.RawGeometry parseGeometry(byte[] data, int modelType, String sourcePath) throws IOException {
        String json = new String(data, StandardCharsets.UTF_8);
        Converter.fromJsonString(json);

        RawYsmModel.RawGeometry geometry = new RawYsmModel.RawGeometry();
        geometry.modelType = modelType;
        geometry.sha256 = sha256Hex(data);
        geometry.sourceJson = data;
        JsonObject root = parseObject(data, sourcePath);
        JsonObject description = findGeometryDescription(root);
        if (description != null) {
            geometry.identifier = getStr(description, "identifier", "");
            geometry.textureWidth = (float) getDouble(description, "texture_width", 64d);
            geometry.textureHeight = (float) getDouble(description, "texture_height", 64d);
            geometry.visibleBoundsWidth = (float) getDouble(description, "visible_bounds_width", 0d);
            geometry.visibleBoundsHeight = (float) getDouble(description, "visible_bounds_height", 0d);
            geometry.visibleBoundsOffset = getFloatArray(description, "visible_bounds_offset", 3);
        }
        JsonArray bones = findGeometryBones(root);
        if (bones != null) {
            for (JsonElement boneElem : bones) {
                if (!boneElem.isJsonObject()) {
                    continue;
                }
                JsonObject boneObj = boneElem.getAsJsonObject();
                RawYsmModel.RawBone bone = new RawYsmModel.RawBone();
                bone.name = getStr(boneObj, "name", "");
                bone.parentName = getStr(boneObj, "parent", "");
                bone.pivot = getFloatArray(boneObj, "pivot", 3);
                bone.rotation = getFloatArray(boneObj, "rotation", 3);
                geometry.bones.add(bone);
            }
        }
        return geometry;
    }

    private RawYsmModel.RawAnimationFile parseAnimations(byte[] data) {
        RawYsmModel.RawAnimationFile file = new RawYsmModel.RawAnimationFile();
        JsonObject root = parseObject(data, "animation");
        if (!hasObject(root, "animations")) {
            return file;
        }
        JsonObject animations = root.getAsJsonObject("animations");
        for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
            RawYsmModel.RawAnimation animation = new RawYsmModel.RawAnimation();
            animation.name = entry.getKey();
            if (entry.getValue().isJsonObject()) {
                JsonObject animObj = entry.getValue().getAsJsonObject();
                animation.length = (float) getDouble(animObj, "animation_length", 0d);
                animation.loopMode = parseLoopMode(animObj.get("loop"));
                if (hasObject(animObj, "bones")) {
                    for (Map.Entry<String, JsonElement> boneEntry : animObj.getAsJsonObject("bones").entrySet()) {
                        RawYsmModel.RawBoneAnimation bone = new RawYsmModel.RawBoneAnimation();
                        bone.boneName = boneEntry.getKey();
                        animation.boneAnimations.add(bone);
                    }
                }
            }
            file.animations.put(animation.name, animation);
        }
        return file;
    }

    private static int parseLoopMode(JsonElement loop) {
        if (loop == null || loop.isJsonNull()) {
            return 0;
        }
        if (loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean()) {
            return loop.getAsBoolean() ? 1 : 0;
        }
        if (loop.isJsonPrimitive()) {
            String value = loop.getAsString();
            if ("true".equals(value) || "loop".equals(value)) {
                return 1;
            }
            if ("hold_on_last_frame".equals(value)) {
                return 2;
            }
        }
        return 0;
    }

    private void parseAnimationControllers(byte[] data,
        Map<String, RawYsmModel.RawAnimationController> targetMap) {
        JsonObject root = parseObject(data, "animation_controller");
        if (!hasObject(root, "animation_controllers")) {
            return;
        }
        JsonObject controllers = root.getAsJsonObject("animation_controllers");
        for (Map.Entry<String, JsonElement> entry : controllers.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject controllerObj = entry.getValue().getAsJsonObject();
            RawYsmModel.RawAnimationController controller = new RawYsmModel.RawAnimationController();
            controller.animationName = entry.getKey();
            controller.initialState = getStr(controllerObj, "initial_state", "");
            if (hasObject(controllerObj, "states")) {
                JsonObject states = controllerObj.getAsJsonObject("states");
                for (Map.Entry<String, JsonElement> stateEntry : states.entrySet()) {
                    if (!stateEntry.getValue().isJsonObject()) {
                        continue;
                    }
                    controller.states.add(parseControllerState(stateEntry.getKey(), stateEntry.getValue().getAsJsonObject()));
                }
            }
            targetMap.put(controller.animationName, controller);
        }
    }

    private RawYsmModel.RawControllerState parseControllerState(String name, JsonObject stateObj) {
        RawYsmModel.RawControllerState state = new RawYsmModel.RawControllerState();
        state.name = name;
        if (hasArray(stateObj, "animations")) {
            for (JsonElement elem : stateObj.getAsJsonArray("animations")) {
                if (elem.isJsonPrimitive()) {
                    state.animations.put(elem.getAsString(), "");
                } else if (elem.isJsonObject()) {
                    copyStringMap(elem.getAsJsonObject(), state.animations);
                }
            }
        }
        if (hasArray(stateObj, "transitions")) {
            for (JsonElement elem : stateObj.getAsJsonArray("transitions")) {
                if (elem.isJsonObject()) {
                    copyStringMap(elem.getAsJsonObject(), state.transitions);
                }
            }
        }
        if (hasArray(stateObj, "on_entry")) {
            addStringArray(stateObj.getAsJsonArray("on_entry"), state.onEntry);
        }
        if (hasArray(stateObj, "on_exit")) {
            addStringArray(stateObj.getAsJsonArray("on_exit"), state.onExit);
        }
        if (hasArray(stateObj, "sound_effects")) {
            for (JsonElement elem : stateObj.getAsJsonArray("sound_effects")) {
                if (elem.isJsonObject()) {
                    state.soundEffects.add(getStr(elem.getAsJsonObject(), "effect", ""));
                } else if (!elem.isJsonNull()) {
                    state.soundEffects.add(elem.getAsString());
                }
            }
        }
        if (stateObj.has("blend_transition")) {
            JsonElement blend = stateObj.get("blend_transition");
            if (blend.isJsonPrimitive() && blend.getAsJsonPrimitive().isNumber()) {
                state.blendTransitionValue = blend.getAsFloat();
            } else if (blend.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : blend.getAsJsonObject().entrySet()) {
                    state.blendTransitions.put(Float.parseFloat(entry.getKey()), entry.getValue().getAsFloat());
                }
            }
        }
        state.blendViaShortestPath = getBool(stateObj, "blend_via_shortest_path", false);
        return state;
    }

    private void parseGlobalResources() throws IOException {
        try (Stream<Path> stream = Files.walk(this.rootPath)) {
            for (Path path : iterable(stream)) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                String relative = normalizeResourcePath(this.rootPath.relativize(path).toString());
                if (relative.startsWith("sounds/") || relative.endsWith(".ogg")) {
                    byte[] data = readResource(relative);
                    if (data != null) {
                        this.model.soundFiles.put(extractFileName(relative), new RawYsmModel.RawDataFile(sha256Hex(data), data));
                    }
                } else if (relative.startsWith("functions/") && relative.endsWith(".molang")) {
                    byte[] data = readResource(relative);
                    if (data != null) {
                        this.model.functionFiles.put(extractFileName(relative), new RawYsmModel.RawDataFile(sha256Hex(data), data));
                    }
                } else if (relative.startsWith("lang/") && relative.endsWith(".json")) {
                    byte[] data = readResource(relative);
                    if (data != null) {
                        this.model.languageFiles.put(parseLocale(relative), parseLanguageFile(data));
                    }
                }
            }
        }
    }

    private RawYsmModel.RawLanguageFile parseLanguageFile(byte[] data) {
        Map<String, String> values = new LinkedHashMap<>();
        JsonObject root = parseObject(data, "lang");
        copyStringMap(root, values);
        return new RawYsmModel.RawLanguageFile(sha256Hex(data), values);
    }

    private void parseLegacyFormat() throws IOException {
        byte[] mainData = readResource(MAIN_JSON);
        byte[] armData = readResource(ARM_JSON);
        if (mainData == null) {
            throw new IOException("Legacy model missing main.json");
        }
        if (armData == null) {
            throw new IOException("Legacy model missing arm.json");
        }
        this.model.mainEntity.mainModel = parseGeometry(mainData, 1, MAIN_JSON);
        this.model.mainEntity.armModel = parseGeometry(armData, 2, ARM_JSON);

        boolean hasTexture = false;
        try (Stream<Path> stream = Files.list(this.rootPath)) {
            for (Path path : iterable(stream)) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                String fileName = path.getFileName().toString();
                if (fileName.endsWith(".png")) {
                    byte[] textureData = readResource(fileName);
                    RawYsmModel.RawTexture texture = parseTexture(fileName, textureData);
                    this.model.mainEntity.textures.put(texture.name, texture);
                    hasTexture = true;
                }
            }
        }
        if (!hasTexture) {
            throw new IOException("Legacy model requires at least one PNG texture");
        }

        readLegacyAnimation(MAIN_ANIMATION_JSON, "main");
        readLegacyAnimation(ARM_ANIMATION_JSON, "arm");
        readLegacyAnimation(EXTRA_ANIMATION_JSON, "extra");
        if (!this.model.mainEntity.textures.isEmpty()) {
            this.model.properties.defaultTexture = this.model.mainEntity.textures.keySet().iterator().next();
        }
    }

    private void readLegacyAnimation(String fileName, String key) throws IOException {
        byte[] data = readResource(fileName);
        if (data == null) {
            return;
        }
        RawYsmModel.RawAnimationFile animationFile = parseAnimations(data);
        animationFile.sourceJson = data;
        animationFile.fileHash = sha256Hex(data);
        animationFile.animType = getAnimTypeFromKey(key);
        this.model.mainEntity.animationFiles.put(key, animationFile);
    }

    private void validateMainPlayerModel() throws IOException {
        if (this.model.mainEntity.mainModel == null) {
            throw new IOException("OpenYSM model missing player main model");
        }
        if (this.model.mainEntity.armModel == null) {
            throw new IOException("OpenYSM model missing player arm model");
        }
        boolean hasPng = false;
        for (RawYsmModel.RawTexture texture : this.model.mainEntity.textures.values()) {
            if (texture.imageFormat == 2 && texture.data != null) {
                hasPng = true;
                break;
            }
        }
        if (!hasPng) {
            throw new IOException("OpenYSM model requires at least one PNG player texture");
        }
    }

    private RawYsmModel.RawImage parseImage(String name, byte[] data) throws IOException {
        ImageMeta meta = parseImageMeta(data);
        RawYsmModel.RawImage image = new RawYsmModel.RawImage();
        image.name = name;
        image.data = data;
        image.width = meta.width;
        image.height = meta.height;
        image.format = meta.format;
        image.unknownFlag = 1;
        image.isPng = meta.format == 2;
        return image;
    }

    private static ImageMeta parseImageMeta(byte[] data) throws IOException {
        int format = detectFormat(data);
        if (format == 2 && data.length >= 24) {
            int width = ((data[16] & 0xFF) << 24) | ((data[17] & 0xFF) << 16) | ((data[18] & 0xFF) << 8)
                | (data[19] & 0xFF);
            int height = ((data[20] & 0xFF) << 24) | ((data[21] & 0xFF) << 16) | ((data[22] & 0xFF) << 8)
                | (data[23] & 0xFF);
            return new ImageMeta(width, height, format);
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image != null) {
            return new ImageMeta(image.getWidth(), image.getHeight(), format);
        }
        return new ImageMeta(0, 0, format);
    }

    public static int detectFormat(byte[] data) {
        if (data == null || data.length < 2) {
            return 0;
        }
        if (data.length >= 2 && data[0] == 0x42 && data[1] == 0x4D) {
            return 1;
        }
        if (data.length >= 8 && (data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E
            && data[3] == 0x47) {
            return 2;
        }
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            return 3;
        }
        if (data.length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
            && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return 4;
        }
        if (data.length >= 12 && data[4] == 'f' && data[5] == 't' && data[6] == 'y' && data[7] == 'p') {
            return 5;
        }
        return 0;
    }

    public static int getAnimTypeFromKey(String key) {
        if ("main".equals(key)) {
            return 1;
        }
        if ("arm".equals(key)) {
            return 2;
        }
        if ("extra".equals(key)) {
            return 3;
        }
        if ("tac".equals(key)) {
            return 4;
        }
        if ("arrow".equals(key)) {
            return 5;
        }
        if ("carryon".equals(key)) {
            return 6;
        }
        if ("parcool".equals(key)) {
            return 7;
        }
        if ("slashblade".equals(key)) {
            return 9;
        }
        if ("tlm".equals(key)) {
            return 10;
        }
        if ("fp_arm".equals(key)) {
            return 11;
        }
        if ("immersive_melodies".equals(key)) {
            return 12;
        }
        if ("irons_spell_books".equals(key) || "iss".equals(key)) {
            return 13;
        }
        return 0;
    }

    public static String getAnimKeyFromType(int type) {
        switch (type) {
            case 1:
                return "main";
            case 2:
                return "arm";
            case 3:
                return "extra";
            case 4:
                return "tac";
            case 5:
                return "arrow";
            case 6:
                return "carryon";
            case 7:
                return "parcool";
            case 9:
                return "slashblade";
            case 10:
                return "tlm";
            case 11:
                return "fp_arm";
            case 12:
                return "immersive_melodies";
            case 13:
                return "irons_spell_books";
            default:
                return "unknown_" + type;
        }
    }

    public static boolean isModelFolder(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }
        if (Files.isRegularFile(dir.resolve(YSM_JSON))) {
            return true;
        }
        return Files.isRegularFile(dir.resolve(MAIN_JSON)) && Files.isRegularFile(dir.resolve(ARM_JSON));
    }

    private static JsonObject findGeometryDescription(JsonObject root) {
        JsonObject geometry = findFirstGeometry(root);
        if (geometry != null && hasObject(geometry, "description")) {
            return geometry.getAsJsonObject("description");
        }
        return null;
    }

    private static JsonArray findGeometryBones(JsonObject root) {
        JsonObject geometry = findFirstGeometry(root);
        if (geometry != null && hasArray(geometry, "bones")) {
            return geometry.getAsJsonArray("bones");
        }
        return null;
    }

    private static JsonObject findFirstGeometry(JsonObject root) {
        if (hasArray(root, "minecraft:geometry")) {
            JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
            for (JsonElement elem : geometries) {
                if (elem.isJsonObject()) {
                    return elem.getAsJsonObject();
                }
            }
        }
        return root;
    }

    private static JsonObject parseObject(byte[] data, String sourceName) {
        JsonElement element = new JsonParser().parse(new String(data, StandardCharsets.UTF_8));
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Expected JSON object in " + sourceName);
        }
        return element.getAsJsonObject();
    }

    private static String[] readStringArray(JsonElement elem) {
        if (elem.isJsonArray()) {
            JsonArray array = elem.getAsJsonArray();
            String[] values = new String[array.size()];
            for (int i = 0; i < array.size(); i++) {
                values[i] = array.get(i).getAsString();
            }
            return values;
        }
        return new String[] { elem.getAsString() };
    }

    private static Iterable<JsonElement> asIterable(JsonElement elem) {
        if (elem == null || elem.isJsonNull()) {
            return Collections.emptyList();
        }
        if (elem.isJsonArray()) {
            return elem.getAsJsonArray();
        }
        return Collections.singletonList(elem);
    }

    private static <T> Iterable<T> iterable(final Stream<T> stream) {
        return stream::iterator;
    }

    private static void copyStringMap(JsonObject source, Map<String, String> target) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            if (!entry.getValue().isJsonNull()) {
                target.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
    }

    private static void addStringArray(JsonArray array, List<String> target) {
        for (JsonElement elem : array) {
            if (!elem.isJsonNull()) {
                target.add(elem.getAsString());
            }
        }
    }

    private static float[] getFloatArray(JsonObject obj, String key, int size) {
        float[] values = new float[size];
        if (hasArray(obj, key)) {
            JsonArray array = obj.getAsJsonArray(key);
            for (int i = 0; i < Math.min(size, array.size()); i++) {
                values[i] = array.get(i).getAsFloat();
            }
        }
        return values;
    }

    private static String parseLocale(String relativePath) {
        String name = relativePath.substring("lang/".length());
        if (name.endsWith(".json")) {
            name = name.substring(0, name.length() - ".json".length());
        }
        return name;
    }

    private static String extractFileName(String fullPath) {
        String fileName = extractFileNameWithExtension(fullPath);
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String extractFileNameWithExtension(String fullPath) {
        String name = normalizeResourcePath(fullPath);
        int slash = name.lastIndexOf('/');
        return slash >= 0 ? name.substring(slash + 1) : name;
    }

    private static boolean hasObject(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonObject();
    }

    private static boolean hasArray(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonArray();
    }

    private static String getStr(JsonObject obj, String key, String def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : def;
    }

    private static boolean getBool(JsonObject obj, String key, boolean def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsBoolean() : def;
    }

    private static double getDouble(JsonObject obj, String key, double def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : def;
    }

    private String calculateFinalFolderHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (Map.Entry<String, String> entry : this.readFilesMd5Map.entrySet()) {
                digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static String md5Hex(byte[] data) {
        return digestHex("MD5", data);
    }

    private static String sha256Hex(byte[] data) {
        return digestHex("SHA-256", data);
    }

    private static String digestHex(String algorithm, byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return toHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " digest is not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] output = new char[bytes.length * 2];
        char[] hex = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            output[i * 2] = hex[value >>> 4];
            output[i * 2 + 1] = hex[value & 0x0F];
        }
        return new String(output);
    }

    private static final class ImageMeta {
        private final int width;
        private final int height;
        private final int format;

        private ImageMeta(int width, int height, int format) {
            this.width = width;
            this.height = height;
            this.format = format;
        }
    }
}
