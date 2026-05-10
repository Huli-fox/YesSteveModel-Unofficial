package com.fox.ysmu.client.sync;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;

import org.apache.commons.io.FileUtils;

import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.model.resource.RawYsmModelAdapter;
import com.fox.ysmu.model.resource.YSMBinaryDeserializer;
import com.fox.ysmu.model.resource.pojo.RawYsmModel;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.C2SCompleteFeedback17;
import com.fox.ysmu.network.message.C2SModelSyncPayload17;
import com.fox.ysmu.util.ThreadTools;
import com.fox.ysmu.ysmu;
import com.google.common.collect.Maps;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.Unpooled;
import rip.ysm.security.YSMByteBuf;
import rip.ysm.security.YSMClientCache;
import rip.ysm.security.YsmCrypt;

@SideOnly(Side.CLIENT)
public final class OpenYsmModelSyncClient {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<UUID, ServerModelContext> SERVER_MODELS = Maps.newConcurrentMap();

    private static volatile int syncStep = 1;
    private static volatile int pendingModelsCount;
    private static volatile int loadedModelsCount;
    private static volatile int downloadedModelsCount;
    private static volatile int cacheHitCount;
    private static byte[] key1;
    private static byte[] lastKey;
    private static byte[] serverKey;
    private static byte[] clientKey;
    private static String currentCacheFolderName;

    private OpenYsmModelSyncClient() {}

    public static void handlePayload(byte[] data) {
        ThreadTools.THREAD_POOL.submit(() -> processServerData(data));
    }

    public static synchronized void resetConnectionState() {
        syncStep = 1;
        pendingModelsCount = 0;
        loadedModelsCount = 0;
        downloadedModelsCount = 0;
        cacheHitCount = 0;
        key1 = null;
        lastKey = null;
        serverKey = null;
        clientKey = null;
        currentCacheFolderName = null;
        SERVER_MODELS.clear();
    }

    public static synchronized void clearConnectionState() {
        resetConnectionState();
    }

    private static synchronized void processServerData(byte[] packetBytes) {
        if (packetBytes == null || packetBytes.length == 0) {
            resetConnectionState();
            return;
        }

        try {
            if (syncStep == 1) {
                byte[] decrypted = YsmCrypt.decrypt(packetBytes, YsmCrypt.publicKey);
                if (decrypted != null) {
                    handlePacket01(decrypted);
                }
            } else if (syncStep == 2) {
                byte[] decrypted = YsmCrypt.decrypt(packetBytes, lastKey);
                if (decrypted != null) {
                    try (YSMByteBuf buf = new YSMByteBuf(Unpooled.wrappedBuffer(decrypted))) {
                        handlePacket03(buf);
                    }
                }
            } else if (syncStep == 3) {
                byte[] decrypted = YsmCrypt.decrypt(packetBytes, key1);
                if (decrypted != null) {
                    try (YSMByteBuf buf = new YSMByteBuf(Unpooled.wrappedBuffer(decrypted))) {
                        handlePacket05(buf);
                    }
                }
            }
        } catch (Exception e) {
            sendComplete(C2SCompleteFeedback17.STATUS_FAILED, e.getClass().getSimpleName() + ": " + e.getMessage());
            ysmu.LOG.warn("OpenYSM client sync error at step " + syncStep, e);
        }
    }

    private static void handlePacket01(byte[] decryptedBuffer) throws Exception {
        resetConnectionState();
        if (decryptedBuffer.length < 56) {
            return;
        }
        key1 = Arrays.copyOfRange(decryptedBuffer, decryptedBuffer.length - 56, decryptedBuffer.length);
        syncStep = 2;

        byte[] garbage = randomGarbage();
        try (YSMByteBuf out = new YSMByteBuf(Unpooled.buffer())) {
            out.writeGarbageHeader(garbage.length, garbage);
            out.writeByte((byte) 0x02);
            out.writeByte((byte) 0x00);
            YsmCrypt.EncryptedPacket encrypted = YsmCrypt.encrypt(out.toArray(), key1, true);
            lastKey = encrypted.nextKey();
            sendPayload(encrypted.data());
        }
    }

    private static void handlePacket03(YSMByteBuf buf) throws Exception {
        buf.skipGarbageHeader();
        int type = buf.readVarInt();
        if (type != 3) {
            return;
        }

        long folderHash = buf.readVarLong();
        currentCacheFolderName = Long.toHexString(folderHash);
        serverKey = new byte[56];
        buf.getRawBuf().readBytes(serverKey);
        clientKey = new byte[56];
        buf.getRawBuf().readBytes(clientKey);

        SERVER_MODELS.clear();
        File cacheDir = getCacheDir();
        if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
            ysmu.LOG.warn("Failed to create OpenYSM client cache directory {}", cacheDir);
        }

        Map<UUID, File> localCacheMap = YSMClientCache.buildCacheIndex(cacheDir, clientKey);
        List<ModelHash> modelsToRequest = new ArrayList<>();
        int serverModelCount = buf.readVarInt();
        ysmu.LOG.info("OpenYSM client received sync index: models={}", serverModelCount);

        for (int i = 0; i < serverModelCount; i++) {
            long hash1 = buf.readVarLong();
            long hash2 = buf.readVarLong();
            String modelId = buf.readString();
            int customSkinModel = buf.readVarInt();
            int version = buf.readVarInt();
            ServerModelContext context = new ServerModelContext(hash1, hash2, modelId, customSkinModel, version);
            SERVER_MODELS.put(context.uuid, context);

            File cachedFile = localCacheMap.get(context.uuid);
            if (YSMClientCache.verifyFileContent(cachedFile, hash1, hash2)) {
                cacheHitCount++;
                byte[] cachedBytes = FileUtils.readFileToByteArray(cachedFile);
                byte[] clearBytes = YsmCrypt.read(cachedBytes, clientKey);
                if (parseAndRegisterModel(clearBytes, context)) {
                    loadedModelsCount++;
                }
                ysmu.LOG.info("OpenYSM client cache hit for {} ({})", modelId, context.uuid);
            } else {
                modelsToRequest.add(new ModelHash(hash1, hash2));
                ysmu.LOG.info("OpenYSM client cache miss for {} ({})", modelId, context.uuid);
            }
        }

        skipPackData(buf);
        sendPacket04(modelsToRequest);
    }

    private static void handlePacket05(YSMByteBuf buf) throws Exception {
        buf.skipGarbageHeader();
        int type = buf.readVarInt();
        if (type != 5) {
            return;
        }

        long hash1 = buf.readVarLong();
        long hash2 = buf.readVarLong();
        UUID uuid = new UUID(hash1, hash2);
        ServerModelContext context = SERVER_MODELS.get(uuid);
        if (context == null) {
            ysmu.LOG.warn("OpenYSM client received unexpected chunk for {}", uuid);
            return;
        }

        int totalSize = buf.readVarInt();
        int chunkOffset = buf.readVarInt();
        int chunkLength = buf.readVarInt();
        if (context.fileBuffer == null) {
            context.fileBuffer = new byte[totalSize];
            context.totalSize = totalSize;
            context.bytesReceived = 0;
        }
        buf.getRawBuf().readBytes(context.fileBuffer, chunkOffset, chunkLength);
        context.bytesReceived += chunkLength;

        if (context.bytesReceived >= context.totalSize) {
            byte[] clientCacheBytes = YsmCrypt
                .transcodeServerDataToClientCache(context.fileBuffer, serverKey, clientKey, hash1, hash2);
            File outFile = new File(getCacheDir(), YSMClientCache.generateCacheFileName(hash1, hash2, clientKey));
            FileUtils.writeByteArrayToFile(outFile, clientCacheBytes);
            context.fileBuffer = null;

            byte[] clearBytes = YsmCrypt.read(clientCacheBytes, clientKey);
            if (parseAndRegisterModel(clearBytes, context)) {
                loadedModelsCount++;
                downloadedModelsCount++;
            }
            pendingModelsCount--;
            ysmu.LOG.info("OpenYSM client downloaded and cached {} to {}", context.modelId, outFile);
            if (pendingModelsCount <= 0) {
                sendComplete(C2SCompleteFeedback17.STATUS_SUCCESS, "");
            }
        }
    }

    private static void sendPacket04(List<ModelHash> modelsToRequest) throws Exception {
        syncStep = 3;
        pendingModelsCount = modelsToRequest.size();

        byte[] garbage = randomGarbage();
        try (YSMByteBuf out = new YSMByteBuf(Unpooled.buffer())) {
            out.writeGarbageHeader(garbage.length, garbage);
            out.writeByte((byte) 0x04);
            out.writeVarInt(modelsToRequest.size());
            for (ModelHash hash : modelsToRequest) {
                out.writeVarLong(hash.hash1);
                out.writeVarLong(hash.hash2);
            }
            sendPayload(YsmCrypt.encrypt(out.toArray(), key1, false).data());
        }

        if (pendingModelsCount == 0) {
            sendComplete(C2SCompleteFeedback17.STATUS_SUCCESS, "");
        }
    }

    private static boolean parseAndRegisterModel(byte[] clearBytes, ServerModelContext context) {
        try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(clearBytes, 32)) {
            RawYsmModel raw = deserializer.deserializeKeepOpen();
            deserializer.parseYSMFooter(raw);
            raw.modelId = context.modelId;
            if (!RawYsmModelAdapter.isBridgeable(raw)) {
                ysmu.LOG.warn("OpenYSM synced model {} is not bridgeable to legacy ModelData", context.modelId);
                return false;
            }
            ModelData data = RawYsmModelAdapter.toLegacyModelData(raw, context.modelId);
            Minecraft.getMinecraft().func_152344_a(() -> ClientModelManager.registerAll(data));
            return true;
        } catch (Exception e) {
            ysmu.LOG.warn("Failed to parse OpenYSM synced model " + context.modelId, e);
            return false;
        }
    }

    private static void skipPackData(YSMByteBuf buf) {
        int packCount = buf.readVarInt();
        for (int i = 0; i < packCount; i++) {
            buf.readString();
            if (buf.readVarInt() != 0) {
                buf.readByteArray();
                buf.readVarInt();
                buf.readVarInt();
                buf.readVarInt();
                buf.readVarInt();
            }
            if (buf.readVarInt() != 0) {
                buf.readString();
                buf.readString();
            }
            int languageCount = buf.readVarInt();
            for (int lang = 0; lang < languageCount; lang++) {
                buf.readString();
                int translationCount = buf.readVarInt();
                for (int entry = 0; entry < translationCount; entry++) {
                    buf.readString();
                    buf.readString();
                }
            }
        }
        if (buf.getRawBuf().readableBytes() > 0) {
            buf.readVarInt();
        }
    }

    private static File getCacheDir() {
        String folder = currentCacheFolderName == null ? "0" : currentCacheFolderName;
        return ServerModelManager.CACHE_CLIENT.resolve(folder).toFile();
    }

    private static void sendPayload(byte[] payload) {
        NetworkHandler.CHANNEL.sendToServer(new C2SModelSyncPayload17(payload));
    }

    private static void sendComplete(int status, String message) {
        NetworkHandler.CHANNEL.sendToServer(
            new C2SCompleteFeedback17(status, loadedModelsCount, downloadedModelsCount, cacheHitCount, message));
        if (status == C2SCompleteFeedback17.STATUS_SUCCESS) {
            ysmu.LOG.info(
                "OpenYSM client sync complete: loaded={}, downloaded={}, cacheHits={}",
                loadedModelsCount,
                downloadedModelsCount,
                cacheHitCount);
        }
        resetConnectionState();
    }

    private static byte[] randomGarbage() {
        byte[] garbage = new byte[16 + RANDOM.nextInt(48)];
        RANDOM.nextBytes(garbage);
        return garbage;
    }

    private static final class ModelHash {

        private final long hash1;
        private final long hash2;

        private ModelHash(long hash1, long hash2) {
            this.hash1 = hash1;
            this.hash2 = hash2;
        }
    }

    private static final class ServerModelContext {

        private final long hash1;
        private final long hash2;
        private final UUID uuid;
        private final String modelId;
        @SuppressWarnings("unused")
        private final int customSkinModel;
        @SuppressWarnings("unused")
        private final int version;
        private byte[] fileBuffer;
        private int totalSize;
        private int bytesReceived;

        private ServerModelContext(long hash1, long hash2, String modelId, int customSkinModel, int version) {
            this.hash1 = hash1;
            this.hash2 = hash2;
            this.uuid = new UUID(hash1, hash2);
            this.modelId = modelId;
            this.customSkinModel = customSkinModel;
            this.version = version;
        }
    }
}
