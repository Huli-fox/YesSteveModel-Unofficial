package rip.ysm.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rip.ysm.algorithms.CityHash;
import rip.ysm.algorithms.YsmZstd;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

class YsmFoundationTest {

    @TempDir
    Path tempDir;

    @Test
    void byteBufRoundTripsLittleEndianAndVarLengthData() {
        byte[] encoded;
        try (YSMByteBuf out = new YSMByteBuf(Unpooled.buffer())) {
            out.writeVarInt(0);
            out.writeVarInt(127);
            out.writeVarInt(128);
            out.writeVarInt(Integer.MAX_VALUE);
            out.writeVarLong(0L);
            out.writeVarLong(300L);
            out.writeVarLong(Long.MAX_VALUE);
            out.writeString("");
            out.writeString("ysm-format");
            out.writeByteArray(new byte[]{1, 2, 3, 4});
            out.writeFloat(1.25F);
            out.writeDword(0x01020304);
            encoded = out.toArray();
        }

        try (YSMByteBuf in = new YSMByteBuf(Unpooled.wrappedBuffer(encoded))) {
            assertEquals(0, in.readVarInt());
            assertEquals(127, in.readVarInt());
            assertEquals(128, in.readVarInt());
            assertEquals(Integer.MAX_VALUE, in.readVarInt());
            assertEquals(0L, in.readVarLong());
            assertEquals(300L, in.readVarLong());
            assertEquals(Long.MAX_VALUE, in.readVarLong());
            assertEquals("", in.readString());
            assertEquals("ysm-format", in.readString());
            assertArrayEquals(new byte[]{1, 2, 3, 4}, in.readByteArray());
            assertEquals(1.25F, in.readFloat(), 0.0F);
            assertEquals(0x01020304L, in.readDword());
        }
    }

    @Test
    void byteBufSkipsGarbageHeader() {
        byte[] encoded;
        try (YSMByteBuf out = new YSMByteBuf(Unpooled.buffer())) {
            out.writeGarbageHeader(3, new byte[]{9, 8, 7});
            out.writeString("after");
            encoded = out.toArray();
        }

        try (YSMByteBuf in = new YSMByteBuf(Unpooled.wrappedBuffer(encoded))) {
            assertEquals(3, in.skipGarbageHeader());
            assertEquals("after", in.readString());
        }
    }

    @Test
    void cityHashMatchesKnownOpenYsmVector() {
        byte[] input = new byte[]{
                (byte) 0x01, (byte) 0x7B, (byte) 0x88, (byte) 0x21, (byte) 0x12, (byte) 0x63, (byte) 0x9A, (byte) 0xEE,
                (byte) 0x6A, (byte) 0xBD, (byte) 0xED, (byte) 0xAD, (byte) 0xA2, (byte) 0xBD, (byte) 0xFC, (byte) 0xF1,
                (byte) 0x9B, (byte) 0xE2, (byte) 0xD6, (byte) 0xF8, (byte) 0xC7, (byte) 0x8F, (byte) 0x0A, (byte) 0xE5,
                (byte) 0x05, (byte) 0x0A, (byte) 0x6B, (byte) 0xE1, (byte) 0x0D, (byte) 0xD4, (byte) 0xEC, (byte) 0xE5,
                (byte) 0x29, (byte) 0xAE, (byte) 0x35, (byte) 0x3C, (byte) 0x54, (byte) 0xE8, (byte) 0x8F, (byte) 0xF4,
                (byte) 0x5C, (byte) 0x80, (byte) 0xCD, (byte) 0x1B, (byte) 0x0F, (byte) 0xD3, (byte) 0x76, (byte) 0xFE,
                (byte) 0x7B, (byte) 0xC2, (byte) 0x41, (byte) 0x65, (byte) 0x52, (byte) 0xA9, (byte) 0x48, (byte) 0xFE,
                (byte) 0xC0, (byte) 0x46, (byte) 0x09, (byte) 0x00, (byte) 0x32, (byte) 0x02, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x58, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x5F, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xB0, (byte) 0xB5, (byte) 0x43, (byte) 0xD5, (byte) 0xFC, (byte) 0x7F, (byte) 0x00, (byte) 0x00,
                (byte) 0x7F, (byte) 0x41, (byte) 0x5B, (byte) 0xB1, (byte) 0x14, (byte) 0x03, (byte) 0xC3, (byte) 0x7D,
                (byte) 0x5E, (byte) 0x73, (byte) 0x40, (byte) 0xEE, (byte) 0x00, (byte) 0x01, (byte) 0x81, (byte) 0x91,
                (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };

        assertEquals(0xcc6f56e6e94aba81L, new CityHash().hash64WithSeed(input, YsmCrypt.SEED_CACHE_DECRYPTION));
    }

    @Test
    void modifiedZstdRoundTripsBinaryData() throws Exception {
        byte[] raw = fixtureBytes(8192);

        byte[] compressed = YsmZstd.compress(raw);
        int magic = ByteBuffer.wrap(compressed, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();

        assertEquals(0xFD2FB528, magic);
        assertArrayEquals(raw, YsmZstd.decompress(compressed.clone()));
    }

    @Test
    void cacheFileNameRoundTripsHashPairAndBuildsIndex() throws Exception {
        byte[] runtimeKey = keyBytes(11);
        long hash1 = 0x0123456789ABCDEFL;
        long hash2 = 0xFEDCBA9876543210L;

        String fileName = YSMClientCache.generateCacheFileName(hash1, hash2, runtimeKey);
        assertNotNull(fileName);
        assertTrue(fileName.matches("[0-9a-f]{40}"));
        assertEquals(new UUID(hash1, hash2), YSMClientCache.getModelUUIDFromFileName(fileName, runtimeKey));

        Path cacheFile = tempDir.resolve(fileName);
        Files.write(cacheFile, new byte[]{1, 2, 3});
        Files.write(tempDir.resolve("not-a-cache-name"), new byte[]{4});

        Map<UUID, File> index = YSMClientCache.buildCacheIndex(tempDir.toFile(), runtimeKey);
        assertEquals(cacheFile.toFile(), index.get(new UUID(hash1, hash2)));
        assertEquals(1, index.size());
    }

    @Test
    void ysmFileEncryptionRoundTripsAndWritesFormat32Crypto3Shell() throws Exception {
        byte[] raw = "format 32 ysm payload".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = YsmCrypt.encryptYsmFile(raw);

        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 0x59, 0x53, 0x47, 0x50}, Arrays.copyOf(encrypted, 7));
        assertEquals(0, encrypted[7]);
        assertEquals(3, ByteBuffer.wrap(encrypted, 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
        assertArrayEquals(raw, YsmCrypt.decryptYsmFile(encrypted));
    }

    @Test
    void serverCacheVerifiesTranscodesAndReadsAsClientCache() throws Exception {
        byte[] raw = fixtureBytes(2048);
        byte[] serverKey = keyBytes(1);
        byte[] clientKey = keyBytes(51);
        long[] hashes = YsmCrypt.calculateModelHashes("phase1/default", serverKey);

        byte[] serverCache = YsmCrypt.encryptServerCache(raw, serverKey, hashes[0], hashes[1]);
        assertTrue(YsmCrypt.verifyServerCache(serverCache, hashes[0], hashes[1]));
        assertArrayEquals(raw, YsmCrypt.read(serverCache, serverKey));

        byte[] clientCache = YsmCrypt.transcodeServerDataToClientCache(serverCache, serverKey, clientKey, hashes[0], hashes[1]);
        assertTrue(YsmCrypt.verifyServerCache(clientCache, hashes[0], hashes[1]));
        assertArrayEquals(raw, YsmCrypt.read(clientCache, clientKey));

        Path cacheFile = tempDir.resolve("client-cache");
        Files.write(cacheFile, clientCache);
        assertTrue(YSMClientCache.verifyFileContent(cacheFile.toFile(), hashes[0], hashes[1]));

        byte[] tampered = clientCache.clone();
        tampered[tampered.length / 2] ^= 0x40;
        assertFalse(YsmCrypt.verifyServerCache(tampered, hashes[0], hashes[1]));
    }

    @Test
    void encryptedPacketRoundTripsOptionalNextKeyAndRejectsTampering() throws Exception {
        byte[] key = keyBytes(91);
        byte[] payload = "packet payload".getBytes(StandardCharsets.UTF_8);

        YsmCrypt.EncryptedPacket withNextKey = YsmCrypt.encrypt(payload, key, true);
        assertNotNull(withNextKey.nextKey());
        assertEquals(56, withNextKey.nextKey().length);
        byte[] decryptedWithNextKey = YsmCrypt.decrypt(withNextKey.data(), key);
        assertArrayEquals(payload, Arrays.copyOf(decryptedWithNextKey, payload.length));
        assertArrayEquals(withNextKey.nextKey(), Arrays.copyOfRange(decryptedWithNextKey, payload.length, decryptedWithNextKey.length));

        YsmCrypt.EncryptedPacket withoutNextKey = YsmCrypt.encrypt(payload, key, false);
        assertNull(withoutNextKey.nextKey());
        assertArrayEquals(payload, YsmCrypt.decrypt(withoutNextKey.data(), key));

        byte[] tampered = withoutNextKey.data().clone();
        tampered[0] ^= 0x01;
        assertThrows(RuntimeException.class, () -> YsmCrypt.decrypt(tampered, key));
    }

    private static byte[] keyBytes(int seed) {
        byte[] key = new byte[56];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (seed + i * 13);
        }
        return key;
    }

    private static byte[] fixtureBytes(int size) {
        byte[] bytes = new byte[size];
        new Random(0x594D534CL + size).nextBytes(bytes);
        return bytes;
    }
}
