package rip.ysm.security;

import rip.ysm.algorithms.CityHash;
import rip.ysm.algorithms.MT19937;
import rip.ysm.algorithms.XChaCha20;
import rip.ysm.algorithms.YsmZstd;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class YsmCrypt {
    public static final long SEED_PACKET_VERIFICATION = 0xEE6FA63D570BD77BL;
    public static final long SEED_KEY_DERIVATION = 0xD017CBBA7B5D3581L;
    public static final long SEED_FILE_VERIFICATION = 0x9E5599DB80C67C29L;
    public static final long SEED_RES_VERIFICATION = 0xA62B1A2C43842BC3L;
    public static final long SEED_CACHE_DECRYPTION = 0xD1C3D1D13A99752BL;
    public static final long SEED_CACHE_VERIFICATION = 0xF346451E53A22261L;

    private static final SecureRandom theRandom = new SecureRandom();
    public static final byte[] publicKey = {
            0x0F, (byte) 0xC7, 0x7E, (byte) 0xF3, (byte) 0xF4, (byte) 0xB8, 0x35, 0x3A, (byte) 0xA2, (byte) 0xBA, 0x7F, (byte) 0xD3, 0x17, 0x79, 0x46, (byte) 0x8E,
            0x65, 0x42, (byte) 0xD0, (byte) 0x98, (byte) 0x8A, (byte) 0x9B, (byte) 0xB0, 0x19, (byte) 0x80, (byte) 0x4F, (byte) 0x81, 0x56, (byte) 0x36, 0x6A, 0x12, (byte) 0x62,
            (byte) 0xBE, 0x0E, (byte) 0xE5, (byte) 0xAD, 0x47, (byte) 0x01, (byte) 0xD4, 0x5E, (byte) 0xE4, (byte) 0xEB, (byte) 0xFB, 0x36, (byte) 0xCB, 0x47, 0x42, (byte) 0x98,
            (byte) 0xF9, (byte) 0xE5, 0x7A, 0x5C, 0x3C, (byte) 0xDB, 0x2C, 0x76
    };

    public static final class EncryptedPacket {
        private final byte[] data;
        private final byte[] nextKey;

        public EncryptedPacket(byte[] data, byte[] nextKey) {
            this.data = data;
            this.nextKey = nextKey;
        }

        public byte[] data() {
            return data;
        }

        public byte[] nextKey() {
            return nextKey;
        }
    }

    public static long[] calculateModelHashes(String modelHashStr, byte[] serverKey) {
        byte[] data = (modelHashStr/* + "111"*/).getBytes(StandardCharsets.UTF_8);
        byte[] xored = mt19937Xor(data, serverKey, SEED_KEY_DERIVATION);
        CityHash ch = new CityHash();
        long hash1 = ch.hash64WithSeed(xored, SEED_CACHE_VERIFICATION);
        long hash2 = ch.hash64WithSeed(xored, SEED_CACHE_DECRYPTION);
        return new long[]{hash1, hash2};
    }

    public static byte[] encryptServerCache(byte[] clearText, byte[] serverKey, long hash1, long hash2) throws Exception {
        byte[] zstdData = YsmZstd.compress(clearText);
        int paddingLength = 16 + theRandom.nextInt(112);
        int randomTop6Bits = theRandom.nextInt(64) << 10;
        int headerWord = (paddingLength & 0x3FF) | randomTop6Bits;
        byte[] payloadToEncrypt = new byte[2 + paddingLength + zstdData.length];
        payloadToEncrypt[0] = (byte) (headerWord & 0xFF);
        payloadToEncrypt[1] = (byte) ((headerWord >> 8) & 0xFF);
        byte[] padding = new byte[paddingLength];
        theRandom.nextBytes(padding);
        System.arraycopy(padding, 0, payloadToEncrypt, 2, paddingLength);
        System.arraycopy(zstdData, 0, payloadToEncrypt, 2 + paddingLength, zstdData.length);
        byte[] chachaKeyS = Arrays.copyOfRange(serverKey, 0, 32);
        byte[] chachaIvS = Arrays.copyOfRange(serverKey, 32, 56);
        byte[] xoredS = mt19937Xor(payloadToEncrypt, serverKey, SEED_KEY_DERIVATION);
        byte[] encryptedPayload = modifiedChaChaEncrypt(xoredS, chachaKeyS, chachaIvS, SEED_CACHE_DECRYPTION);
        try (YSMByteBuf headerBuf = new YSMByteBuf(Unpooled.buffer());) {
            headerBuf.writeVarInt(1);
            headerBuf.writeVarInt(0);
            headerBuf.writeVarInt(0);
            headerBuf.writeVarInt(0);
            headerBuf.writeVarInt(32); // format
            headerBuf.writeVarInt(0);
            headerBuf.writeVarInt(0);
            headerBuf.writeVarInt(0);
            headerBuf.writeVarInt(0);

            byte[] headers = new byte[headerBuf.getRawBuf().readableBytes()];
            headerBuf.getRawBuf().readBytes(headers);

            int finalPayloadLen = headers.length + encryptedPayload.length;
            ByteBuffer finalBuf = ByteBuffer.allocate(finalPayloadLen + 8).order(ByteOrder.LITTLE_ENDIAN);

            finalBuf.put(headers);
            finalBuf.put(encryptedPayload);

            byte[] dataToHash = Arrays.copyOfRange(finalBuf.array(), 0, finalPayloadLen);
            CityHash ch = new CityHash();
            long calculatedHash = ch.hash64WithSeed(dataToHash, SEED_CACHE_VERIFICATION);
            long realHash = calculatedHash ^ hash1 ^ hash2; // 签名

            finalBuf.putLong(realHash);

            return finalBuf.array();
        }
    }

    public static boolean verifyServerCache(byte[] cacheData, long hash1, long hash2) {
        if (cacheData.length < 8) return false;
        int payloadEnd = cacheData.length - 8;
        byte[] payload = Arrays.copyOfRange(cacheData, 0, payloadEnd);
        long fileSignature = ByteBuffer.wrap(cacheData, payloadEnd, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        CityHash ch = new CityHash();
        long calculatedHash = ch.hash64WithSeed(payload, SEED_CACHE_VERIFICATION);
        long expectedSignature = calculatedHash ^ hash1 ^ hash2;
        return fileSignature == expectedSignature;
    }

    public static byte[] encryptYsmFile(byte[] rawClearText) throws Exception {
        byte[] key = new byte[32];
        byte[] iv = new byte[24];
        theRandom.nextBytes(key);
        theRandom.nextBytes(iv);

        byte[] keyIv = new byte[56];
        System.arraycopy(key, 0, keyIv, 0, 32);
        System.arraycopy(iv, 0, keyIv, 32, 24);

        byte[] zstdData = YsmZstd.compress(rawClearText);
        int paddingLength = 16 + theRandom.nextInt(112);
        int randomTop6Bits = theRandom.nextInt(64) << 10;
        int headerWord = (paddingLength & 0x3FF) | randomTop6Bits;
        byte[] payloadToEncrypt = new byte[2 + paddingLength + zstdData.length];
        payloadToEncrypt[0] = (byte) (headerWord & 0xFF);
        payloadToEncrypt[1] = (byte) ((headerWord >> 8) & 0xFF);
        byte[] padding = new byte[paddingLength];
        theRandom.nextBytes(padding);
        System.arraycopy(padding, 0, payloadToEncrypt, 2, paddingLength);
        System.arraycopy(zstdData, 0, payloadToEncrypt, 2 + paddingLength, zstdData.length);
        byte[] xoredData = mt19937Xor(payloadToEncrypt, keyIv, SEED_KEY_DERIVATION);
        byte[] encryptedBinaryData = modifiedChaChaEncrypt(xoredData, key, iv, SEED_RES_VERIFICATION);
        byte[] prefix = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 0x59, 0x53, 0x47, 0x50};
        byte[] data = new byte[0];

        byte[] headerBytes = new byte[prefix.length + data.length];
        System.arraycopy(prefix, 0, headerBytes, 0, prefix.length);
        System.arraycopy(data, 0, headerBytes, prefix.length, data.length);

        int totalSizeWithoutHash = headerBytes.length + 1 + 4 + encryptedBinaryData.length + 56;
        ByteBuffer fileBuf = ByteBuffer.allocate(totalSizeWithoutHash + 8).order(ByteOrder.LITTLE_ENDIAN);

        fileBuf.put(headerBytes);
        fileBuf.put((byte) 0x00); // terminator

        fileBuf.putInt(3);
        fileBuf.put(encryptedBinaryData);
        fileBuf.put(key);
        fileBuf.put(iv);

        byte[] dataToHash = new byte[totalSizeWithoutHash];
        System.arraycopy(fileBuf.array(), 0, dataToHash, 0, totalSizeWithoutHash);

        CityHash ch = new CityHash();
        long fileHash = ch.hash64WithSeed(dataToHash, SEED_FILE_VERIFICATION);
        fileBuf.putLong(fileHash);

        return fileBuf.array();
    }


    public static byte[] transcodeServerDataToClientCache(byte[] serverData, byte[] serverKey, byte[] clientKey, long hash1, long hash2) throws Exception {

        try (YSMByteBuf buf = new YSMByteBuf(Unpooled.wrappedBuffer(serverData))) {
            int headerStart = buf.getRawBuf().readerIndex();
            if (buf.readVarInt() != 1) throw new RuntimeException("Invalid YSM cache format");
            buf.readVarInt();
            buf.readVarInt();
            buf.readVarInt();
            int format = buf.readVarInt();
            if (format != 32) throw new RuntimeException("Unsupported YSM cache format: " + format);
            buf.readVarInt();
            buf.readVarInt();
            buf.readVarInt();
            buf.readVarInt();

            int headerEnd = buf.getRawBuf().readerIndex();
            int payloadEnd = serverData.length - 8;
            if (payloadEnd <= headerEnd) {
                throw new RuntimeException("Invalid server payload size!");
            }

            byte[] headers = Arrays.copyOfRange(serverData, headerStart, headerEnd);
            byte[] serverEncryptedPayload = Arrays.copyOfRange(serverData, headerEnd, payloadEnd);

            // packet shell
            byte[] chachaKeyS = Arrays.copyOfRange(serverKey, 0, 32);
            byte[] chachaIvS = Arrays.copyOfRange(serverKey, 32, 56);

            byte[] chachaDecS = modifiedChaChaDecrypt(serverEncryptedPayload, chachaKeyS, chachaIvS, SEED_CACHE_DECRYPTION);
            byte[] plainText = mt19937Xor(chachaDecS, serverKey, SEED_KEY_DERIVATION);

            // local shell
            byte[] chachaKeyC = Arrays.copyOfRange(clientKey, 0, 32);
            byte[] chachaIvC = Arrays.copyOfRange(clientKey, 32, 56);

            byte[] xoredC = mt19937Xor(plainText, clientKey, SEED_KEY_DERIVATION);
            byte[] clientEncryptedPayload = modifiedChaChaEncrypt(xoredC, chachaKeyC, chachaIvC, SEED_CACHE_DECRYPTION);

            int finalPayloadLen = headers.length + clientEncryptedPayload.length;
            ByteBuffer finalBuf = ByteBuffer.allocate(finalPayloadLen + 8).order(ByteOrder.LITTLE_ENDIAN);
            finalBuf.put(headers);
            finalBuf.put(clientEncryptedPayload);

            byte[] dataToHash = Arrays.copyOfRange(finalBuf.array(), 0, finalPayloadLen);
            CityHash ch = new CityHash();
            long calculatedHash = ch.hash64WithSeed(dataToHash, SEED_CACHE_VERIFICATION);
            long realHash = calculatedHash ^ hash1 ^ hash2;

            finalBuf.putLong(realHash);
            return finalBuf.array();
        }
    }

    private static byte[] modifiedChaChaEncrypt(byte[] plainText, byte[] key, byte[] iv, long seed) throws Exception {
        byte[] keyIv = new byte[56];
        System.arraycopy(key, 0, keyIv, 0, 32);
        System.arraycopy(iv, 0, keyIv, 32, 24);

        CityHash ch = new CityHash();
        long hash2 = ch.hash64WithSeed(keyIv, seed);

        int nextRoundSize = (int) (((hash2 & 0x3FL) | 0x40L) << 6);
        int rounds = (int) (10 * Long.remainderUnsigned(hash2, 3) + 10);

        XChaCha20 ctx = new XChaCha20(key, iv, rounds);
        byte[] result = new byte[plainText.length];
        int blockPointer = 0;

        while (blockPointer < plainText.length) {
            if (blockPointer + nextRoundSize > plainText.length) {
                nextRoundSize = plainText.length - blockPointer;
            }

            byte[] plainChunk = Arrays.copyOfRange(plainText, blockPointer, blockPointer + nextRoundSize);
            byte[] encChunk = ctx.processBytes(plainChunk, 0, nextRoundSize);
            System.arraycopy(encChunk, 0, result, blockPointer, nextRoundSize);

            blockPointer += nextRoundSize;

            if (blockPointer < plainText.length) {
                long resHash = ch.hash64WithSeed(plainChunk, seed);
                nextRoundSize = ctx.updateStateYSM(resHash);
            }
        }
        return result;
    }

    public static byte[] decryptYsmFile(byte[] fileData) throws Exception {
        if (fileData.length < 8 + 24 + 32 + 8) {
            throw new RuntimeException("Invalid YSM file: File too short.");
        }

        int headerLength = 0;
        while (headerLength < fileData.length && fileData[headerLength] != 0x00) {
            headerLength++;
        }

        int tailOffset = fileData.length - 64;
        byte[] key = Arrays.copyOfRange(fileData, tailOffset, tailOffset + 32);
        byte[] iv = Arrays.copyOfRange(fileData, tailOffset + 32, tailOffset + 56);
        long fileHash = ByteBuffer.wrap(fileData, tailOffset + 56, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();

        CityHash ch = new CityHash();
        long calculatedHash = ch.hash64WithSeed(Arrays.copyOfRange(fileData, 0, fileData.length - 8), SEED_FILE_VERIFICATION);
        if (calculatedHash != fileHash) {
            throw new RuntimeException("Corrupted YSM file: File hash mismatch.");
        }

        int ptrBinaryData = headerLength + 1;
        int crypto = ByteBuffer.wrap(fileData, ptrBinaryData, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (crypto != 3) {
            throw new RuntimeException("Invalid YSM file: Crypto version is not 3.");
        }
        ptrBinaryData += 4;

        byte[] encryptedBinaryData = Arrays.copyOfRange(fileData, ptrBinaryData, tailOffset);
        byte[] chachaDecrypted = modifiedChaChaDecrypt(encryptedBinaryData, key, iv, SEED_RES_VERIFICATION);

        byte[] keyIv = new byte[56];
        System.arraycopy(key, 0, keyIv, 0, 32);
        System.arraycopy(iv, 0, keyIv, 32, 24);
        byte[] xorredData = mt19937Xor(chachaDecrypted, keyIv, SEED_KEY_DERIVATION);

        //uint16_t n = xorred_data[0] | (xorred_data[1] << 8); n &= 0x3ff;
        int n = ((xorredData[0] & 0xFF) | ((xorredData[1] & 0xFF) << 8)) & 0x3FF;

        int zstdOffset = 2 + n;
        byte[] bytes = Arrays.copyOfRange(xorredData, zstdOffset, xorredData.length);

        return YsmZstd.decompress(bytes);
    }

    private static byte[] modifiedChaChaDecrypt(byte[] data, byte[] key, byte[] iv, long seed) throws Exception {
        byte[] keyIv = new byte[56];
        System.arraycopy(key, 0, keyIv, 0, 32);
        System.arraycopy(iv, 0, keyIv, 32, 24);

        CityHash ch = new CityHash();
        long hash2 = ch.hash64WithSeed(keyIv, seed);

        // ((hash2 & 0x3f) | 0x40) << 6
        int nextRoundSize = (int) (((hash2 & 0x3FL) | 0x40L) << 6);
        int rounds = (int) (10 * Long.remainderUnsigned(hash2, 3) + 10);

        XChaCha20 ctx = new XChaCha20(key, iv, rounds);

        byte[] result = new byte[data.length];
        int blockPointer = 0;

        while (blockPointer < data.length) {
            if (blockPointer + nextRoundSize > data.length) {
                nextRoundSize = data.length - blockPointer;
            }
            byte[] decChunk = ctx.processBytes(data, blockPointer, nextRoundSize);
            System.arraycopy(decChunk, 0, result, blockPointer, nextRoundSize);
            blockPointer += nextRoundSize;

            if (blockPointer < data.length) {
                long resHash = ch.hash64WithSeed(decChunk, seed);
                nextRoundSize = ctx.updateStateYSM(resHash);
            }
        }

        return result;
    }

    public static byte[] decrypt(byte[] packet, byte[] key) throws Exception {
        if (packet.length <= 11) throw new RuntimeException("Packet too short!");

        int payloadLen = packet.length - 8;
        byte[] payload = Arrays.copyOfRange(packet, 0, payloadLen);
        long packetHash = ByteBuffer.wrap(packet, payloadLen, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();

        CityHash ch = new CityHash();
        long calculatedHash = ch.hash64WithSeed(payload, SEED_PACKET_VERIFICATION);
        if (calculatedHash != packetHash) throw new RuntimeException("Integrity compromised");

        byte[] xoredData = mt19937Xor(payload, key, SEED_KEY_DERIVATION);

        byte[] chachaKey = Arrays.copyOfRange(key, 0, 32);
        byte[] chachaIv = Arrays.copyOfRange(key, 32, 56);
        XChaCha20 chacha = new XChaCha20(chachaKey, chachaIv, 30);

        return chacha.processBytes(xoredData, 0, xoredData.length);
    }

    public static EncryptedPacket encrypt(byte[] payload, byte[] currentKeyIv, boolean appendNextKey) throws Exception {
        byte[] fullPlaintext;
        byte[] nextKeyIv = null;

        if (appendNextKey) {
            nextKeyIv = new byte[56];
            theRandom.nextBytes(nextKeyIv);
            fullPlaintext = new byte[payload.length + 56];
            System.arraycopy(payload, 0, fullPlaintext, 0, payload.length);
            System.arraycopy(nextKeyIv, 0, fullPlaintext, payload.length, 56);
        } else {
            fullPlaintext = payload;
        }

        byte[] key = Arrays.copyOfRange(currentKeyIv, 0, 32);
        byte[] iv = Arrays.copyOfRange(currentKeyIv, 32, 56);
        byte[] step1Encrypted = new XChaCha20(key, iv, 30).processBytes(fullPlaintext, 0, fullPlaintext.length);
        byte[] step2Xorred = mt19937Xor(step1Encrypted, currentKeyIv, SEED_KEY_DERIVATION);

        long hash = new CityHash().hash64WithSeed(step2Xorred, SEED_PACKET_VERIFICATION);

        ByteBuffer finalPacket = ByteBuffer.allocate(step2Xorred.length + 8).order(ByteOrder.LITTLE_ENDIAN);
        finalPacket.put(step2Xorred);
        finalPacket.putLong(hash);

        return new EncryptedPacket(finalPacket.array(), nextKeyIv);
    }

    private static byte[] mt19937Xor(byte[] data, byte[] currentKeyIv, long seedDerivation) {
        long mtSeed = new CityHash().hash64WithSeed(currentKeyIv, seedDerivation);
        MT19937 mt = new MT19937(mtSeed);
        byte[] result = new byte[data.length];

        int i = 0;
        while (i < data.length) {
            long rnd = mt.extract_number();
            for (int j = 0; j < 8 && i < data.length; ++j) {
                byte keystreamByte = (byte) ((rnd >>> (j * 8)) & 0xFF);
                result[i] = (byte) (data[i] ^ keystreamByte);
                i++;
            }
        }
        return result;
    }

    public static byte[] read(byte[] cacheFileData, byte[] clientKey) throws Exception {
        try (YSMByteBuf buf = new YSMByteBuf(Unpooled.wrappedBuffer(cacheFileData))) {
            buf.readVarInt();
            buf.readVarInt();
            buf.readVarInt();
            buf.readVarInt();
            buf.readVarInt();
            buf.readVarInt();
            buf.readVarInt();
            buf.readVarInt();
            buf.readVarInt();
            int headerEnd = buf.getRawBuf().readerIndex();

            int payloadEnd = cacheFileData.length - 8;
            if (payloadEnd <= headerEnd) {
                throw new RuntimeException("Cache file is too small or corrupted!");
            }
            byte[] encryptedPayload = Arrays.copyOfRange(cacheFileData, headerEnd, payloadEnd);

            byte[] chachaKeyC = Arrays.copyOfRange(clientKey, 0, 32);
            byte[] chachaIvC = Arrays.copyOfRange(clientKey, 32, 56);

            byte[] chachaDec = modifiedChaChaDecrypt(encryptedPayload, chachaKeyC, chachaIvC, SEED_CACHE_DECRYPTION);
            byte[] plainText = mt19937Xor(chachaDec, clientKey, SEED_KEY_DERIVATION);

            int n = ((plainText[0] & 0xFF) | ((plainText[1] & 0xFF) << 8)) & 0x3FF;
            int zstdOffset = 2 + n;

            byte[] zstdData = Arrays.copyOfRange(plainText, zstdOffset, plainText.length);

            return YsmZstd.decompress(zstdData);
        }
    }
}
