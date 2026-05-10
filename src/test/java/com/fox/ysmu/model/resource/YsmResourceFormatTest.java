package com.fox.ysmu.model.resource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import io.netty.buffer.Unpooled;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fox.ysmu.data.EncryptTools;
import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.model.format.Type;
import com.fox.ysmu.model.resource.pojo.RawYsmModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import rip.ysm.security.YSMByteBuf;

class YsmResourceFormatTest {

    private static final byte[] PNG_1X1 = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=");

    @TempDir
    Path tempDir;

    @Test
    void folderDeserializerReadsOpenYsmFolderAndBridgeablePlayerFiles() throws Exception {
        Path modelDir = tempDir.resolve("Fancy Model");
        Files.createDirectories(modelDir.resolve("models"));
        Files.createDirectories(modelDir.resolve("textures"));
        Files.createDirectories(modelDir.resolve("animations"));
        Files.createDirectories(modelDir.resolve("controller"));
        Files.createDirectories(modelDir.resolve("lang"));
        Files.write(modelDir.resolve("ysm.json"), ysmJson().getBytes(StandardCharsets.UTF_8));
        Files.write(modelDir.resolve("models/main.json"), geometryJson("geometry.test.main").getBytes(StandardCharsets.UTF_8));
        Files.write(modelDir.resolve("models/arm.json"), geometryJson("geometry.test.arm").getBytes(StandardCharsets.UTF_8));
        Files.write(modelDir.resolve("textures/default.png"), PNG_1X1);
        Files.write(modelDir.resolve("animations/main.animation.json"), animationJson().getBytes(StandardCharsets.UTF_8));
        Files.write(modelDir.resolve("controller/main_controllers.json"), controllerJson().getBytes(StandardCharsets.UTF_8));
        Files.write(
            modelDir.resolve("lang/en_us.json"),
            "{\"model.ysm.test\":\"Test Model\",\"properties.extra_animation.extra0\":\"Wave\"}"
                .getBytes(StandardCharsets.UTF_8));

        RawYsmModel raw;
        try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(modelDir)) {
            raw = deserializer.deserialize();
            assertEquals(32, deserializer.getFolderHash().length());
        }

        assertEquals("Sample", raw.metadata.name);
        assertEquals("default", raw.properties.defaultTexture);
        assertNotNull(raw.mainEntity.mainModel.sourceJson);
        assertNotNull(raw.mainEntity.armModel.sourceJson);
        assertTrue(raw.mainEntity.textures.containsKey("default"));
        assertTrue(raw.languageFiles.containsKey("en_us"));
        assertEquals(1, raw.mainEntity.animationControllerFiles.size());
        assertTrue(RawYsmModelAdapter.isBridgeable(raw));

        RawYsmModel.RawTexture jpegTexture = new RawYsmModel.RawTexture();
        jpegTexture.name = "preview";
        jpegTexture.sourceFileName = "preview.jpg";
        jpegTexture.imageFormat = 3;
        jpegTexture.data = new byte[] { (byte) 0xFF, (byte) 0xD8, 0x00 };
        raw.mainEntity.textures.put(jpegTexture.name, jpegTexture);

        ModelData data = RawYsmModelAdapter.toLegacyModelData(raw, "fancy_model");
        JsonObject description = getDescription(data.getModel().get("main"));
        assertEquals(0.9d, description.get("ysm_height_scale").getAsDouble(), 0.0001d);
        assertEquals(0.8d, description.get("ysm_width_scale").getAsDouble(), 0.0001d);
        JsonObject extraInfo = description.getAsJsonObject("ysm_extra_info");
        assertEquals("Sample", extraInfo.get("name").getAsString());
        assertEquals("Bridge metadata", extraInfo.get("tips").getAsString());
        assertEquals("CC0", extraInfo.get("license").getAsString());
        assertEquals("Tester (author)", extraInfo.getAsJsonArray("authors").get(0).getAsString());
        assertEquals("Wave", extraInfo.getAsJsonArray("extra_animation_names").get(0).getAsString());
        assertArrayEquals(PNG_1X1, data.getTexture().get("default.png"));
        assertFalse(data.getTexture().containsKey("preview.jpg"));
        assertTrue(data.getAnimation().containsKey("main"));
        assertTrue(data.getAnimation().containsKey("arm"));
        assertTrue(data.getAnimation().containsKey("extra"));
        assertTrue(data.getAnimation().containsKey(YsmControllerResources.ANIMATION_MAP_PREFIX + "main_controllers"));
    }

    @Test
    void rawGeometryWithoutSourceJsonCanGenerateLegacyGeometryJson() throws Exception {
        RawYsmModel source = new RawYsmModel();
        source.metadata.name = "Generated";
        source.properties.widthScale = 1.2f;
        source.properties.heightScale = 1.1f;
        source.mainEntity.mainModel = geometryWithFlatCube(1, "geometry.generated.main");
        source.mainEntity.armModel = geometryWithFlatCube(2, "geometry.generated.arm");
        RawYsmModel.RawTexture texture = new RawYsmModel.RawTexture();
        texture.name = "default";
        texture.sourceFileName = "default.png";
        texture.imageFormat = 2;
        texture.data = PNG_1X1;
        source.mainEntity.textures.put(texture.name, texture);

        assertTrue(RawYsmModelAdapter.isBridgeable(source));

        ModelData data = RawYsmModelAdapter.toLegacyModelData(source, "generated_model");
        JsonObject description = getDescription(data.getModel().get("main"));
        assertEquals("geometry.generated.main", description.get("identifier").getAsString());
        assertEquals(1.1d, description.get("ysm_height_scale").getAsDouble(), 0.0001d);
        JsonObject geometry = new JsonParser().parse(new String(data.getModel().get("main"), StandardCharsets.UTF_8))
            .getAsJsonObject()
            .getAsJsonArray("minecraft:geometry")
            .get(0)
            .getAsJsonObject();
        JsonObject polyMesh = geometry.getAsJsonArray("bones")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("poly_mesh");
        assertEquals("quad_list", polyMesh.get("polys").getAsString());
        assertTrue(polyMesh.get("normalized_uvs").getAsBoolean());
        assertEquals(12, polyMesh.getAsJsonArray("positions").size());
        assertEquals(8, polyMesh.getAsJsonArray("uvs").size());
    }

    @Test
    void legacyEncryptedModelPreservesMultiTextureByteOrder() throws Exception {
        Map<String, byte[]> model = new LinkedHashMap<>();
        model.put("main", geometryJson("geometry.test.main").getBytes(StandardCharsets.UTF_8));
        model.put("arm", geometryJson("geometry.test.arm").getBytes(StandardCharsets.UTF_8));

        byte[] defaultTexture = new byte[] { 0, 1, 2, 3, 4, 5, 6 };
        byte[] blueTexture = new byte[] { 10, 11, 12 };
        Map<String, byte[]> textures = new LinkedHashMap<>();
        textures.put("default.png", defaultTexture);
        textures.put("blue.png", blueTexture);

        Map<String, byte[]> animations = new LinkedHashMap<>();
        animations.put("main", animationJson().getBytes(StandardCharsets.UTF_8));

        EncryptTools.createRandomPassword();
        byte[] rawPassword = EncryptTools.writePassword();
        byte[] playerKey = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
        byte[] encryptedPassword = EncryptTools.encryptPassword(playerKey, rawPassword);
        byte[] encryptedModel = EncryptTools.assembleEncryptModels(
            new ModelData("ordered_textures", Type.FOLDER, model, textures, animations));

        ModelData decoded = EncryptTools.decryptModel(playerKey, encryptedPassword, encryptedModel);

        assertNotNull(decoded);
        assertArrayEquals(defaultTexture, decoded.getTexture().get("default.png"));
        assertArrayEquals(blueTexture, decoded.getTexture().get("blue.png"));
    }

    @Test
    void binarySerializerRoundTripsFormat32RawModel() throws Exception {
        RawYsmModel source = new RawYsmModel();
        source.formatVersion = 32;
        source.properties.sha256 = "0123456789abcdef";
        source.properties.defaultTexture = "default";
        source.metadata.name = "Binary Sample";
        source.mainEntity.mainModel = geometry(1, "geometry.main");
        source.mainEntity.armModel = geometry(2, "geometry.arm");
        RawYsmModel.RawTexture texture = new RawYsmModel.RawTexture();
        texture.name = "default";
        texture.sourceFileName = "default.png";
        texture.hash = "texture-hash";
        texture.width = 1;
        texture.height = 1;
        texture.imageFormat = 2;
        texture.unknownFlag = 1;
        texture.data = PNG_1X1;
        source.mainEntity.textures.put(texture.name, texture);

        byte[] bytes;
        try (YSMByteBuf serialized = YSMBinarySerializer.serialize(source, 32, false)) {
            bytes = serialized.toArray();
        }

        RawYsmModel decoded;
        try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(bytes, 32)) {
            decoded = deserializer.deserialize();
        }

        assertEquals(32, decoded.formatVersion);
        assertEquals("Binary Sample", decoded.metadata.name);
        assertEquals("default", decoded.properties.defaultTexture);
        assertNotNull(decoded.mainEntity.mainModel);
        assertNotNull(decoded.mainEntity.armModel);
        assertTrue(decoded.mainEntity.textures.containsKey("default"));
    }

    @Test
    void binaryDeserializerKeepsOpenYsmExtensionAnimationTypesSeparate() throws Exception {
        RawYsmModel source = new RawYsmModel();
        source.formatVersion = 32;
        source.properties.sha256 = "0123456789abcdef";
        source.properties.defaultTexture = "default";
        source.metadata.name = "Extension Animations";
        source.mainEntity.mainModel = geometry(1, "geometry.main");
        source.mainEntity.armModel = geometry(2, "geometry.arm");
        RawYsmModel.RawTexture texture = new RawYsmModel.RawTexture();
        texture.name = "default";
        texture.sourceFileName = "default.png";
        texture.hash = "texture-hash";
        texture.width = 1;
        texture.height = 1;
        texture.imageFormat = 2;
        texture.unknownFlag = 1;
        texture.data = PNG_1X1;
        source.mainEntity.textures.put(texture.name, texture);
        source.mainEntity.animationFiles.put(
            "slashblade",
            animationFile(9, "slashblade-hash", "hold_mainhand:slashblade"));
        source.mainEntity.animationFiles.put(
            "tac",
            animationFile(4, "tac-hash", "tac:aim$tacz:minigun"));

        byte[] bytes;
        try (YSMByteBuf serialized = YSMBinarySerializer.serialize(source, 32, false)) {
            bytes = serialized.toArray();
        }

        RawYsmModel decoded;
        try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(bytes, 32)) {
            decoded = deserializer.deserialize();
        }

        assertEquals("slashblade", YSMFolderDeserializer.getAnimKeyFromType(9));
        assertEquals("tac", YSMFolderDeserializer.getAnimKeyFromType(4));
        assertEquals("carryon", YSMFolderDeserializer.getAnimKeyFromType(6));
        assertEquals("parcool", YSMFolderDeserializer.getAnimKeyFromType(7));
        assertEquals("tlm", YSMFolderDeserializer.getAnimKeyFromType(10));
        assertEquals("immersive_melodies", YSMFolderDeserializer.getAnimKeyFromType(12));
        assertEquals("irons_spell_books", YSMFolderDeserializer.getAnimKeyFromType(13));
        assertEquals("unknown_99", YSMFolderDeserializer.getAnimKeyFromType(99));
        assertEquals(9, YSMFolderDeserializer.getAnimTypeFromKey("slashblade"));
        assertEquals(4, YSMFolderDeserializer.getAnimTypeFromKey("tac"));
        assertEquals(6, YSMFolderDeserializer.getAnimTypeFromKey("carryon"));
        assertEquals(7, YSMFolderDeserializer.getAnimTypeFromKey("parcool"));
        assertEquals(10, YSMFolderDeserializer.getAnimTypeFromKey("tlm"));
        assertEquals(12, YSMFolderDeserializer.getAnimTypeFromKey("immersive_melodies"));
        assertEquals(13, YSMFolderDeserializer.getAnimTypeFromKey("irons_spell_books"));
        assertTrue(decoded.mainEntity.animationFiles.containsKey("slashblade"));
        assertTrue(decoded.mainEntity.animationFiles.containsKey("tac"));
        assertFalse(decoded.mainEntity.animationFiles.containsKey("unknown"));
        assertTrue(decoded.mainEntity.animationFiles.get("slashblade")
            .animations
            .containsKey("hold_mainhand:slashblade"));
        assertTrue(decoded.mainEntity.animationFiles.get("tac").animations.containsKey("tac:aim$tacz:minigun"));
    }

    @Test
    void binaryAnimationsWithoutSourceJsonGenerateLegacyAnimationJson() throws Exception {
        RawYsmModel source = new RawYsmModel();
        source.formatVersion = 32;
        source.properties.sha256 = "0123456789abcdef";
        source.properties.defaultTexture = "default";
        source.mainEntity.mainModel = geometryWithFlatCube(1, "geometry.main");
        source.mainEntity.armModel = geometryWithFlatCube(2, "geometry.arm");
        RawYsmModel.RawTexture texture = new RawYsmModel.RawTexture();
        texture.name = "default";
        texture.sourceFileName = "default.png";
        texture.hash = "texture-hash";
        texture.width = 1;
        texture.height = 1;
        texture.imageFormat = 2;
        texture.unknownFlag = 1;
        texture.data = PNG_1X1;
        source.mainEntity.textures.put(texture.name, texture);

        RawYsmModel.RawAnimationFile animationFile = new RawYsmModel.RawAnimationFile();
        animationFile.animType = 1;
        RawYsmModel.RawAnimation idle = new RawYsmModel.RawAnimation();
        idle.name = "idle";
        idle.length = 1.0f;
        idle.loopMode = 1;
        RawYsmModel.RawBoneAnimation sceneBone = new RawYsmModel.RawBoneAnimation();
        sceneBone.boneName = "SceneRoot";
        RawYsmModel.RawKeyframe hiddenScale = new RawYsmModel.RawKeyframe();
        hiddenScale.timestamp = 0.0f;
        hiddenScale.postData = new Object[] { 0f, 0f, 0f };
        sceneBone.scale.add(hiddenScale);
        idle.boneAnimations.add(sceneBone);
        RawYsmModel.RawBoneAnimation tailBone = new RawYsmModel.RawBoneAnimation();
        tailBone.boneName = "Tail";
        RawYsmModel.RawKeyframe tailStart = new RawYsmModel.RawKeyframe();
        tailStart.timestamp = 0.0f;
        tailStart.interpolationMode = 2;
        tailStart.postData = new Object[] { 0f, "math.sin(query.anim_time*360)*30", 0f };
        tailBone.rotation.add(tailStart);
        idle.boneAnimations.add(tailBone);
        RawYsmModel.RawBoneAnimation blendedBone = new RawYsmModel.RawBoneAnimation();
        blendedBone.boneName = "Blended";
        RawYsmModel.RawKeyframe blendStart = new RawYsmModel.RawKeyframe();
        blendStart.timestamp = 0.0f;
        blendStart.interpolationMode = 2;
        blendStart.postData = new Object[] { 0f, 0f, 0f };
        blendedBone.rotation.add(blendStart);
        RawYsmModel.RawKeyframe blendEnd = new RawYsmModel.RawKeyframe();
        blendEnd.timestamp = 0.5f;
        blendEnd.interpolationMode = 2;
        blendEnd.postData = new Object[] { 10f, 0f, 0f };
        blendedBone.rotation.add(blendEnd);
        idle.boneAnimations.add(blendedBone);
        animationFile.animations.put(idle.name, idle);
        source.mainEntity.animationFiles.put("main", animationFile);

        byte[] bytes;
        try (YSMByteBuf serialized = YSMBinarySerializer.serialize(source, 32, false)) {
            bytes = serialized.toArray();
        }

        RawYsmModel decoded;
        try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(bytes, 32)) {
            decoded = deserializer.deserialize();
        }

        assertNull(decoded.mainEntity.animationFiles.get("main").sourceJson);

        ModelData data = RawYsmModelAdapter.toLegacyModelData(decoded, "binary_anim");
        JsonObject root = new JsonParser().parse(new String(data.getAnimation().get("main"), StandardCharsets.UTF_8))
            .getAsJsonObject();
        JsonObject idleJson = root.getAsJsonObject("animations").getAsJsonObject("idle");
        assertTrue(idleJson.get("loop").getAsBoolean());
        JsonArray scale = idleJson.getAsJsonObject("bones")
            .getAsJsonObject("SceneRoot")
            .getAsJsonArray("scale");
        assertEquals(0.0d, scale.get(0).getAsDouble(), 0.0001d);
        assertEquals(0.0d, scale.get(1).getAsDouble(), 0.0001d);
        assertEquals(0.0d, scale.get(2).getAsDouble(), 0.0001d);
        JsonArray tailRotation = idleJson.getAsJsonObject("bones")
            .getAsJsonObject("Tail")
            .getAsJsonArray("rotation");
        assertEquals("math.sin(query.anim_time*360)*30", tailRotation.get(1).getAsString());
        JsonObject blendedRotation = idleJson.getAsJsonObject("bones")
            .getAsJsonObject("Blended")
            .getAsJsonObject("rotation");
        assertFalse(blendedRotation.getAsJsonObject("0.0").has("vector"));
        assertTrue(blendedRotation.getAsJsonObject("0.0").has("post"));
        assertEquals("catmullrom", blendedRotation.getAsJsonObject("0.5").get("lerp_mode").getAsString());
    }

    @Test
    void binaryDeserializerAcceptsAbbreviatedSyncFooter() throws Exception {
        RawYsmModel source = new RawYsmModel();
        source.formatVersion = 32;
        source.properties.sha256 = "0123456789abcdef";
        source.properties.defaultTexture = "default";
        source.metadata.name = "Footer Sample";
        source.mainEntity.mainModel = geometry(1, "geometry.main");
        source.mainEntity.armModel = geometry(2, "geometry.arm");
        RawYsmModel.RawTexture texture = new RawYsmModel.RawTexture();
        texture.name = "default";
        texture.sourceFileName = "default.png";
        texture.hash = "texture-hash";
        texture.width = 1;
        texture.height = 1;
        texture.imageFormat = 2;
        texture.unknownFlag = 1;
        texture.data = PNG_1X1;
        source.mainEntity.textures.put(texture.name, texture);

        byte[] bytes;
        try (YSMByteBuf serialized = YSMBinarySerializer.serialize(source, 32, true)) {
            bytes = serialized.toArray();
        }

        try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(bytes, 32)) {
            RawYsmModel decoded = deserializer.deserializeKeepOpen();
            deserializer.parseYSMFooter(decoded);

            assertEquals(65535, decoded.footer.version);
            assertEquals(0, decoded.footer.unkInt1);
            assertEquals(0L, decoded.footer.time);
        }
    }

    @Test
    void binaryDeserializerReadsFormat15AndBridgeConvertsRgbaTexture() throws Exception {
        byte[] rgbaRedPixel = new byte[] { (byte) 0xFF, 0, 0, (byte) 0xFF };

        RawYsmModel decoded;
        try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(format15Binary(rgbaRedPixel))) {
            decoded = deserializer.deserialize();
        }

        assertEquals(15, decoded.formatVersion);
        assertEquals("Legacy Binary", decoded.metadata.name);
        assertTrue(RawYsmModelAdapter.isBridgeable(decoded));

        ModelData data = RawYsmModelAdapter.toLegacyModelData(decoded, "_name_e58aa8e58a9be88782");
        byte[] texture = data.getTexture().get("texture.png");
        assertNotNull(texture);
        assertEquals((byte) 0x89, texture[0]);
        assertEquals((byte) 0x50, texture[1]);
        assertEquals((byte) 0x4E, texture[2]);
        assertEquals((byte) 0x47, texture[3]);
        assertEquals("Legacy Binary", getDescription(data.getModel().get("main"))
            .getAsJsonObject("ysm_extra_info")
            .get("name")
            .getAsString());
    }

    private static RawYsmModel.RawGeometry geometry(int type, String identifier) {
        RawYsmModel.RawGeometry geometry = new RawYsmModel.RawGeometry();
        geometry.modelType = type;
        geometry.identifier = identifier;
        geometry.sha256 = identifier + "-hash";
        geometry.textureWidth = 64f;
        geometry.textureHeight = 64f;
        geometry.visibleBoundsOffset = new float[] { 0f, 1.5f, 0f };
        return geometry;
    }

    private static byte[] format15Binary(byte[] rgbaTexture) {
        try (YSMByteBuf buf = new YSMByteBuf(Unpooled.buffer())) {
            buf.writeDword(15);
            buf.writeVarInt(0);

            buf.writeVarInt(2);
            writeFormat15GeometryEntry(buf, 1, "geometry.legacy.main");
            writeFormat15GeometryEntry(buf, 2, "geometry.legacy.arm");

            buf.writeVarInt(0);
            buf.writeVarInt(0);
            buf.writeVarInt(0);

            buf.writeVarInt(1);
            buf.writeString("texture");
            buf.writeByteArray(rgbaTexture);
            buf.writeVarInt(1);
            buf.writeVarInt(1);
            buf.writeVarInt(0);

            buf.writeVarInt(0);
            buf.writeVarInt(0);
            buf.writeVarInt(0);
            buf.writeVarInt(0);
            buf.writeVarInt(0);
            buf.writeVarInt(0);

            buf.writeString("format15-sha");
            buf.writeVarInt(1);
            buf.writeVarInt(0);
            buf.writeString("Legacy Binary");
            buf.writeString("Format 15 sample");
            buf.writeString("CC0");
            buf.writeString("");
            buf.writeVarInt(0);
            buf.writeVarInt(0);
            buf.writeFloat(0.8f);
            buf.writeFloat(0.9f);
            buf.writeVarInt(0);
            buf.writeVarInt(0);
            buf.writeVarInt(0);
            buf.writeString("texture");
            buf.writeString("");
            buf.writeVarInt(1);
            buf.writeVarInt(0);
            buf.writeVarInt(0);
            buf.writeVarInt(0);

            return buf.toArray();
        }
    }

    private static void writeFormat15GeometryEntry(YSMByteBuf buf, int modelType, String identifier) {
        buf.writeVarInt(modelType);
        buf.writeVarInt(1);
        writeFormat15Geometry(buf, identifier);
    }

    private static void writeFormat15Geometry(YSMByteBuf buf, String identifier) {
        buf.writeVarInt(1);
        buf.writeString("");
        buf.writeVarInt(1);
        buf.writeVarInt(1);
        writeVector(buf, 0f, 0f, -1f);
        writeVertex(buf, -0.5f, 0f, 0f, 0f, 0f);
        writeVertex(buf, 0.5f, 0f, 0f, 1f, 0f);
        writeVertex(buf, 0.5f, 1f, 0f, 1f, 1f);
        writeVertex(buf, -0.5f, 1f, 0f, 0f, 1f);
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        buf.writeString("root");
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        writeVector(buf, 0f, 0f, 0f);
        writeVector(buf, 0f, 0f, 0f);

        buf.writeString(identifier);
        buf.writeFloat(64f);
        buf.writeFloat(64f);
        buf.writeFloat(3f);
        buf.writeFloat(2f);
        buf.writeVarInt(3);
        buf.writeFloat(0f);
        buf.writeFloat(1.5f);
        buf.writeFloat(0f);
        buf.writeFloat(0f);
        buf.writeFloat(0f);
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        buf.writeVarInt(0);
    }

    private static void writeVertex(YSMByteBuf buf, float x, float y, float z, float u, float v) {
        writeVector(buf, x, y, z);
        buf.writeFloat(u);
        buf.writeFloat(v);
    }

    private static void writeVector(YSMByteBuf buf, float x, float y, float z) {
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
    }

    private static RawYsmModel.RawGeometry geometryWithFlatCube(int type, String identifier) {
        RawYsmModel.RawGeometry geometry = geometry(type, identifier);
        RawYsmModel.RawBone bone = new RawYsmModel.RawBone();
        bone.name = "root";
        RawYsmModel.RawCube cube = new RawYsmModel.RawCube();
        RawYsmModel.RawFace face = new RawYsmModel.RawFace();
        face.normal = new float[] { 0f, 0f, -1f };
        face.positions = new float[][] {
            { -0.5f, 0f, 0f },
            { 0.5f, 0f, 0f },
            { 0.5f, 1f, 0f },
            { -0.5f, 1f, 0f } };
        face.u = new float[] { 0f, 1f, 1f, 0f };
        face.v = new float[] { 0f, 0f, 1f, 1f };
        cube.faces.add(face);
        bone.cubes.add(cube);
        geometry.bones.add(bone);
        return geometry;
    }

    private static RawYsmModel.RawAnimationFile animationFile(int type, String hash, String animationName) {
        RawYsmModel.RawAnimationFile animationFile = new RawYsmModel.RawAnimationFile();
        animationFile.animType = type;
        animationFile.fileHash = hash;
        RawYsmModel.RawAnimation animation = new RawYsmModel.RawAnimation();
        animation.name = animationName;
        animation.length = 1.0f;
        animation.loopMode = 1;
        animationFile.animations.put(animation.name, animation);
        return animationFile;
    }

    private static JsonObject getDescription(byte[] geometryJson) {
        return new JsonParser().parse(new String(geometryJson, StandardCharsets.UTF_8))
            .getAsJsonObject()
            .getAsJsonArray("minecraft:geometry")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("description");
    }

    private static String ysmJson() {
        return "{"
            + "\"metadata\":{\"name\":\"Sample\",\"tips\":\"Bridge metadata\",\"license\":{\"type\":\"CC0\"},"
            + "\"authors\":[{\"name\":\"Tester\",\"role\":\"author\"}]},"
            + "\"properties\":{\"default_texture\":\"default\",\"width_scale\":0.8,\"height_scale\":0.9,"
            + "\"extra_animation\":{\"extra0\":\"\",\"extra1\":\"Spin\"}},"
            + "\"files\":{\"player\":{"
            + "\"model\":{\"main\":\"models/main.json\",\"arm\":\"models/arm.json\"},"
            + "\"texture\":[\"textures/default.png\"],"
            + "\"animation\":{\"main\":\"animations/main.animation.json\"},"
            + "\"animation_controllers\":[\"controller/main_controllers.json\"]"
            + "}}"
            + "}";
    }

    private static String geometryJson(String identifier) {
        return "{"
            + "\"format_version\":\"1.12.0\","
            + "\"minecraft:geometry\":[{\"description\":{"
            + "\"identifier\":\"" + identifier + "\","
            + "\"texture_width\":64,\"texture_height\":64,"
            + "\"visible_bounds_width\":2,\"visible_bounds_height\":3,"
            + "\"visible_bounds_offset\":[0,1.5,0]"
            + "},\"bones\":[{\"name\":\"root\",\"pivot\":[0,0,0]}]}]"
            + "}";
    }

    private static String animationJson() {
        return "{"
            + "\"format_version\":\"1.8.0\","
            + "\"animations\":{\"idle\":{\"loop\":true,\"animation_length\":1.0,\"bones\":{\"root\":{\"rotation\":[0,0,0]}}}}"
            + "}";
    }

    private static String controllerJson() {
        return "{"
            + "\"format_version\":\"1.19.0\","
            + "\"animation_controllers\":{\"player.main\":{\"initial_state\":\"default\",\"states\":{"
            + "\"default\":{\"animations\":[\"idle\"],\"transitions\":[{\"walk\":\"q.ground_speed>0.05\"}]},"
            + "\"walk\":{\"animations\":[\"walk\"],\"transitions\":[{\"default\":\"q.ground_speed<=0.05\"}],"
            + "\"blend_transition\":0.15,\"on_entry\":[\"v.entered_walk=1;\"]}"
            + "}}}}";
    }
}
