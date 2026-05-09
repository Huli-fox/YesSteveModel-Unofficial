package com.fox.ysmu.model.resource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.model.resource.pojo.RawYsmModel;

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
        Files.createDirectories(modelDir.resolve("lang"));
        Files.write(modelDir.resolve("ysm.json"), ysmJson().getBytes(StandardCharsets.UTF_8));
        Files.write(modelDir.resolve("models/main.json"), geometryJson("geometry.test.main").getBytes(StandardCharsets.UTF_8));
        Files.write(modelDir.resolve("models/arm.json"), geometryJson("geometry.test.arm").getBytes(StandardCharsets.UTF_8));
        Files.write(modelDir.resolve("textures/default.png"), PNG_1X1);
        Files.write(modelDir.resolve("animations/main.animation.json"), animationJson().getBytes(StandardCharsets.UTF_8));
        Files.write(modelDir.resolve("lang/en_us.json"), "{\"model.ysm.test\":\"Test Model\"}".getBytes(StandardCharsets.UTF_8));

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
        assertTrue(RawYsmModelAdapter.isBridgeable(raw));

        ModelData data = RawYsmModelAdapter.toLegacyModelData(raw, "fancy_model");
        assertArrayEquals(raw.mainEntity.mainModel.sourceJson, data.getModel().get("main"));
        assertArrayEquals(PNG_1X1, data.getTexture().get("default.png"));
        assertTrue(data.getAnimation().containsKey("main"));
        assertTrue(data.getAnimation().containsKey("arm"));
        assertTrue(data.getAnimation().containsKey("extra"));
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

    private static String ysmJson() {
        return "{"
            + "\"metadata\":{\"name\":\"Sample\",\"authors\":[{\"name\":\"Tester\",\"role\":\"author\"}]},"
            + "\"properties\":{\"default_texture\":\"default\",\"width_scale\":0.8,\"height_scale\":0.9},"
            + "\"files\":{\"player\":{"
            + "\"model\":{\"main\":\"models/main.json\",\"arm\":\"models/arm.json\"},"
            + "\"texture\":[\"textures/default.png\"],"
            + "\"animation\":{\"main\":\"animations/main.animation.json\"}"
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
}
