package com.fox.ysmu.network.sync;

import java.io.File;
import java.io.FileOutputStream;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;

import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.model.resource.RawYsmModelAdapter;
import com.fox.ysmu.model.resource.YSMBinaryDeserializer;
import com.fox.ysmu.model.resource.pojo.RawYsmModel;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.sync17.C2SCompleteFeedback17;
import com.fox.ysmu.network.message.sync17.C2SModelSyncPayload17;
import com.fox.ysmu.util.ThreadTools;
import com.fox.ysmu.ysmu;

import io.netty.buffer.Unpooled;
import rip.ysm.security.YSMByteBuf;
import rip.ysm.security.YSMClientCache;
import rip.ysm.security.YsmCrypt;

public final class ModelSyncClient17 {

    private static final Object LOCK = new Object();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static int syncStep = 1;
    private static byte[] key1;
    private static byte[] lastKey;
    private static byte[] serverKey;
    private static byte[] clientKey;
    private static String currentCacheFolderName;
    private static int pendingModelsCount;
    private static final Map<ModelHash17, ClientModelContext> SERVER_MODELS = new HashMap<>();

    private ModelSyncClient17() {}

    public static void startVersionedSync() {
        synchronized (LOCK) {
            resetStateLocked();
        }
        ClientModelManager.startOpenYsmSync17();
    }

    public static void handlePayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            synchronized (LOCK) {
                resetStateLocked();
            }
            return;
        }
        ThreadTools.THREAD_POOL.submit(() -> handlePayloadAsync(payload));
    }

    static byte[] buildPacket02(byte[] key) throws Exception {
        try (YSMByteBuf out = new YSMByteBuf(Unpooled.buffer())) {
            writeGarbageHeader(out);
            out.writeByte((byte) 0x02);
            out.writeByte((byte) 0x00);
            YsmCrypt.EncryptedPacket packet = YsmCrypt.encrypt(out.toArray(), key, true);
            synchronized (LOCK) {
                lastKey = packet.nextKey();
            }
            return packet.data();
        }
    }

    private static void handlePayloadAsync(byte[] payload) {
        try {
            int step;
            synchronized (LOCK) {
                step = syncStep;
            }
            if (step == 1) {
                handlePacket01(payload);
            } else if (step == 2) {
                handlePacket03(payload);
            } else if (step == 3) {
                handlePacket05(payload);
            }
        } catch (Exception e) {
            ysmu.LOG.warn("YSM sync17 client error", e);
            sendFeedback(false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void handlePacket01(byte[] payload) throws Exception {
        byte[] decrypted = YsmCrypt.decrypt(payload, YsmCrypt.publicKey);
        if (decrypted == null || decrypted.length < 56) {
            return;
        }
        byte[] nextKey = new byte[56];
        System.arraycopy(decrypted, decrypted.length - 56, nextKey, 0, 56);
        synchronized (LOCK) {
            key1 = nextKey;
            syncStep = 2;
        }
        sendPayload(buildPacket02(nextKey));
    }

    private static void handlePacket03(byte[] payload) throws Exception {
        byte[] decryptKey;
        synchronized (LOCK) {
            decryptKey = lastKey;
        }
        byte[] decrypted = YsmCrypt.decrypt(payload, decryptKey);
        java.util.List<ModelHash17> misses = new java.util.ArrayList<>();
        try (YSMByteBuf in = new YSMByteBuf(Unpooled.wrappedBuffer(decrypted))) {
            in.skipGarbageHeader();
            int type = in.readVarInt();
            if (type != 3) {
                return;
            }
            long folderHash = in.readVarLong();
            currentCacheFolderName = Long.toHexString(folderHash);
            serverKey = new byte[56];
            in.getRawBuf().readBytes(serverKey);
            clientKey = new byte[56];
            in.getRawBuf().readBytes(clientKey);
            File cacheDir = ServerModelManager.CACHE_CLIENT.resolve(currentCacheFolderName).toFile();
            if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
                ysmu.LOG.warn("Failed to create YSM sync17 client cache dir {}", cacheDir);
            }
            Map<UUID, File> localCache = YSMClientCache.buildCacheIndex(cacheDir, clientKey);
            int modelCount = in.readVarInt();
            synchronized (LOCK) {
                SERVER_MODELS.clear();
            }
            for (int i = 0; i < modelCount; i++) {
                ModelHash17 hash = new ModelHash17(in.readVarLong(), in.readVarLong());
                String modelId = in.readString();
                in.readVarInt();
                in.readVarInt();
                ClientModelContext context = new ClientModelContext(hash, modelId);
                synchronized (LOCK) {
                    SERVER_MODELS.put(hash, context);
                }
                File cachedFile = localCache.get(hash.asUuid());
                if (YSMClientCache.verifyFileContent(cachedFile, hash.getHash1(), hash.getHash2())) {
                    byte[] cacheFile = java.nio.file.Files.readAllBytes(cachedFile.toPath());
                    parseAndRegister(YsmCrypt.read(cacheFile, clientKey), modelId);
                } else {
                    misses.add(hash);
                }
            }
            int packCount = in.readVarInt();
            for (int i = 0; i < packCount; i++) {
                skipPack(in);
            }
        }
        synchronized (LOCK) {
            pendingModelsCount = misses.size();
            syncStep = 3;
        }
        sendPacket04(misses);
        if (misses.isEmpty()) {
            sendFeedback(true, "cache-hit");
        }
    }

    private static void handlePacket05(byte[] payload) throws Exception {
        byte[] decryptKey;
        synchronized (LOCK) {
            decryptKey = key1;
        }
        byte[] decrypted = YsmCrypt.decrypt(payload, decryptKey);
        ClientModelContext context;
        byte[] completeServerCache = null;
        try (YSMByteBuf in = new YSMByteBuf(Unpooled.wrappedBuffer(decrypted))) {
            in.skipGarbageHeader();
            int type = in.readVarInt();
            if (type != 5) {
                return;
            }
            ModelHash17 hash = new ModelHash17(in.readVarLong(), in.readVarLong());
            int totalSize = in.readVarInt();
            int offset = in.readVarInt();
            int length = in.readVarInt();
            byte[] chunk = new byte[length];
            in.getRawBuf().readBytes(chunk);
            synchronized (LOCK) {
                context = SERVER_MODELS.get(hash);
                if (context == null) {
                    return;
                }
                if (context.fileBuffer == null) {
                    context.fileBuffer = new byte[totalSize];
                    context.totalSize = totalSize;
                    context.bytesReceived = 0;
                }
                if (offset < 0 || length < 0 || offset + length > context.fileBuffer.length) {
                    return;
                }
                if (context.receivedOffsets.add(Integer.valueOf(offset))) {
                    System.arraycopy(chunk, 0, context.fileBuffer, offset, length);
                    context.bytesReceived += length;
                }
                if (context.bytesReceived >= context.totalSize) {
                    completeServerCache = context.fileBuffer;
                    context.fileBuffer = null;
                }
            }
        }
        if (completeServerCache != null) {
            finishDownloadedModel(context, completeServerCache);
        }
    }

    private static void finishDownloadedModel(ClientModelContext context, byte[] serverCache) throws Exception {
        byte[] clientCache = YsmCrypt.transcodeServerDataToClientCache(
            serverCache,
            serverKey,
            clientKey,
            context.hash.getHash1(),
            context.hash.getHash2());
        File cacheDir = ServerModelManager.CACHE_CLIENT.resolve(currentCacheFolderName).toFile();
        if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
            ysmu.LOG.warn("Failed to create YSM sync17 client cache dir {}", cacheDir);
        }
        String fileName = YSMClientCache.generateCacheFileName(context.hash.getHash1(), context.hash.getHash2(), clientKey);
        try (FileOutputStream output = new FileOutputStream(new File(cacheDir, fileName))) {
            output.write(clientCache);
        }
        parseAndRegister(YsmCrypt.read(clientCache, clientKey), context.modelId);
        boolean done;
        synchronized (LOCK) {
            pendingModelsCount--;
            done = pendingModelsCount <= 0;
        }
        if (done) {
            sendFeedback(true, "downloaded");
        }
    }

    private static void sendPacket04(java.util.List<ModelHash17> misses) throws Exception {
        byte[] encryptKey;
        synchronized (LOCK) {
            encryptKey = key1;
        }
        try (YSMByteBuf out = new YSMByteBuf(Unpooled.buffer())) {
            writeGarbageHeader(out);
            out.writeByte((byte) 0x04);
            out.writeVarInt(misses.size());
            for (ModelHash17 hash : misses) {
                out.writeVarLong(hash.getHash1());
                out.writeVarLong(hash.getHash2());
            }
            YsmCrypt.EncryptedPacket packet = YsmCrypt.encrypt(out.toArray(), encryptKey, false);
            sendPayload(packet.data());
        }
    }

    private static void parseAndRegister(byte[] rawBytes, String modelId) throws Exception {
        RawYsmModel raw;
        try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(rawBytes, 32)) {
            raw = deserializer.deserializeKeepOpen();
            deserializer.parseYSMFooter(raw);
        }
        ModelData data = RawYsmModelAdapter.toLegacyModelData(raw, modelId);
        Minecraft.getMinecraft().func_152344_a(() -> ClientModelManager.registerAll(data));
    }

    private static void skipPack(YSMByteBuf in) {
        in.readString();
        if (in.readVarInt() != 0) {
            in.readByteArray();
            in.readVarInt();
            in.readVarInt();
            in.readVarInt();
            in.readVarInt();
        }
        if (in.readVarInt() != 0) {
            in.readString();
            in.readString();
        }
        int languageCount = in.readVarInt();
        for (int i = 0; i < languageCount; i++) {
            in.readString();
            int keyCount = in.readVarInt();
            for (int j = 0; j < keyCount; j++) {
                in.readString();
                in.readString();
            }
        }
    }

    private static void sendPayload(byte[] data) {
        NetworkHandler.CHANNEL.sendToServer(new C2SModelSyncPayload17(data));
    }

    private static void sendFeedback(boolean success, String message) {
        NetworkHandler.CHANNEL.sendToServer(new C2SCompleteFeedback17(success, message));
        if (success) {
            synchronized (LOCK) {
                resetStateLocked();
            }
        }
    }

    private static void writeGarbageHeader(YSMByteBuf out) {
        int length = 16 + RANDOM.nextInt(48);
        byte[] garbage = new byte[length];
        RANDOM.nextBytes(garbage);
        out.writeGarbageHeader(length, garbage);
    }

    private static void resetStateLocked() {
        syncStep = 1;
        key1 = null;
        lastKey = null;
        serverKey = null;
        clientKey = null;
        currentCacheFolderName = null;
        pendingModelsCount = 0;
        SERVER_MODELS.clear();
    }

    private static final class ClientModelContext {

        private final ModelHash17 hash;
        private final String modelId;
        private byte[] fileBuffer;
        private int totalSize;
        private int bytesReceived;
        private final Set<Integer> receivedOffsets = new HashSet<>();

        private ClientModelContext(ModelHash17 hash, String modelId) {
            this.hash = hash;
            this.modelId = modelId;
        }
    }
}
