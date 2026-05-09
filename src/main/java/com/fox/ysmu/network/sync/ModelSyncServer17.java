package com.fox.ysmu.network.sync;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.fox.ysmu.Config;
import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.model.resource.YSMBinarySerializer;
import com.fox.ysmu.model.resource.pojo.RawYsmModel;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.RequestSyncModel;
import com.fox.ysmu.network.message.sync17.S2CModelSyncPayload17;
import com.fox.ysmu.network.message.sync17.S2CVersionCheck17;
import com.fox.ysmu.util.ThreadTools;
import com.fox.ysmu.ysmu;

import io.netty.buffer.Unpooled;
import rip.ysm.security.YSMByteBuf;
import rip.ysm.security.YsmCrypt;

public final class ModelSyncServer17 {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int FORMAT = 32;
    private static final int CHUNK_SIZE = 32_000;
    private static final long VERSION_FALLBACK_MILLIS = 3_000L;
    private static final Map<java.util.UUID, PlayerSyncState17> SYNC_STATES = new ConcurrentHashMap<>();
    private static final Map<String, ServerModelData17> SERVER_MODELS = new ConcurrentHashMap<>();

    private ModelSyncServer17() {}

    public static void clearModelCaches() {
        SERVER_MODELS.clear();
    }

    public static void requestSync(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        PlayerSyncState17 state = new PlayerSyncState17(RANDOM);
        fillStableClientKey(state.clientKey, serverPlayer.getUniqueID());
        SYNC_STATES.put(serverPlayer.getUniqueID(), state);
        NetworkHandler.sendToClientPlayer(new S2CVersionCheck17(S2CVersionCheck17.PROTOCOL_VERSION), serverPlayer);
        ThreadTools.THREAD_POOL.submit(() -> fallbackIfVersionCheckTimesOut(serverPlayer, state));
    }

    public static void requestLegacySync(EntityPlayer player) {
        NetworkHandler.sendToClientPlayer(new RequestSyncModel(), player);
    }

    public static void clearPlayer(EntityPlayer player) {
        if (player != null) {
            SYNC_STATES.remove(player.getUniqueID());
        }
    }

    public static void handleVersionCheck(EntityPlayerMP player, int protocolVersion, boolean supported) {
        PlayerSyncState17 state = SYNC_STATES.get(player.getUniqueID());
        if (state == null) {
            return;
        }
        state.touch();
        state.versionAcknowledged = true;
        if (!supported || protocolVersion != S2CVersionCheck17.PROTOCOL_VERSION) {
            SYNC_STATES.remove(player.getUniqueID());
            requestLegacySync(player);
            return;
        }
        if (SERVER_MODELS.isEmpty()) {
            SYNC_STATES.remove(player.getUniqueID());
            requestLegacySync(player);
            return;
        }
        initiateHandshake(player, state);
    }

    public static void handlePayload(EntityPlayerMP player, byte[] payload) {
        if (payload == null || payload.length == 0) {
            clearPlayer(player);
            return;
        }
        ThreadTools.THREAD_POOL.submit(() -> handlePayloadAsync(player, payload));
    }

    public static void handleCompleteFeedback(EntityPlayerMP player, boolean success, String message) {
        clearPlayer(player);
        if (success) {
            ysmu.LOG.info("YSM sync17 completed for {}", player.getCommandSenderName());
        } else {
            ysmu.LOG.warn("YSM sync17 failed for {}: {}", player.getCommandSenderName(), message);
            requestLegacySync(player);
        }
    }

    public static void cacheRawModel(String modelId, RawYsmModel raw, ModelData legacyData) {
        if (raw == null || ServerModelManager.OPEN_YSM_SERVER_KEY == null) {
            return;
        }
        try {
            ensureStableModelHash(modelId, raw, legacyData);
            long[] hashes = YsmCrypt.calculateModelHashes(raw.properties.sha256, ServerModelManager.OPEN_YSM_SERVER_KEY);
            ModelHash17 modelHash = new ModelHash17(hashes[0], hashes[1]);
            byte[] serialized;
            try (YSMByteBuf buf = YSMBinarySerializer.serialize(raw, FORMAT, true)) {
                serialized = buf.toArray();
            }
            byte[] encrypted = YsmCrypt.encryptServerCache(
                serialized,
                ServerModelManager.OPEN_YSM_SERVER_KEY,
                modelHash.getHash1(),
                modelHash.getHash2());
            java.nio.file.Files.write(ServerModelManager.CACHE_SERVER.resolve(modelHash.toServerCacheFileName()), encrypted);
            boolean customSkin = "misc/2_steve".equals(modelId) || "misc/1_alex".equals(modelId);
            SERVER_MODELS.put(
                modelId,
                new ServerModelData17(modelId, raw.properties.sha256, modelHash, FORMAT, customSkin,
                    legacyData == null ? Collections.emptySet() : legacyData.getTexture().keySet(), raw));
        } catch (Exception e) {
            ysmu.LOG.warn("Failed to create OpenYSM sync17 cache for model {}", modelId, e);
        }
    }

    private static void fallbackIfVersionCheckTimesOut(EntityPlayerMP player, PlayerSyncState17 state) {
        try {
            Thread.sleep(VERSION_FALLBACK_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        PlayerSyncState17 current = SYNC_STATES.get(player.getUniqueID());
        if (current == state && !current.versionAcknowledged) {
            SYNC_STATES.remove(player.getUniqueID());
            requestLegacySync(player);
        }
    }

    private static void initiateHandshake(EntityPlayerMP player, PlayerSyncState17 state) {
        ThreadTools.THREAD_POOL.submit(() -> {
            try {
                state.allowedModels.clear();
                state.allowedModels.addAll(SERVER_MODELS.values());
                state.step = 1;
                state.touch();
                try (YSMByteBuf out = new YSMByteBuf(Unpooled.buffer())) {
                    writeGarbageHeader(out);
                    out.writeByte((byte) 0x01);
                    YsmCrypt.EncryptedPacket packet = YsmCrypt.encrypt(out.toArray(), YsmCrypt.publicKey, true);
                    state.key1 = packet.nextKey();
                    sendPayload(player, packet.data());
                }
            } catch (Exception e) {
                ysmu.LOG.warn("Failed to initiate YSM sync17 for " + player.getCommandSenderName(), e);
                clearPlayer(player);
                requestLegacySync(player);
            }
        });
    }

    private static void handlePayloadAsync(EntityPlayerMP player, byte[] payload) {
        PlayerSyncState17 state = SYNC_STATES.get(player.getUniqueID());
        if (state == null || isTimedOut(state)) {
            clearPlayer(player);
            return;
        }
        state.touch();
        try {
            if (state.step == 1) {
                handlePacket02(player, state, payload);
            } else if (state.step == 2) {
                handlePacket04(player, state, payload);
            }
        } catch (Exception e) {
            ysmu.LOG.warn("YSM sync17 server error for " + player.getCommandSenderName(), e);
            clearPlayer(player);
            requestLegacySync(player);
        }
    }

    private static void handlePacket02(EntityPlayerMP player, PlayerSyncState17 state, byte[] payload) throws Exception {
        byte[] decrypted = YsmCrypt.decrypt(payload, state.key1);
        if (decrypted == null || decrypted.length < 56) {
            return;
        }
        state.clientNextKey = new byte[56];
        System.arraycopy(decrypted, decrypted.length - 56, state.clientNextKey, 0, 56);
        byte[] body = new byte[decrypted.length - 56];
        System.arraycopy(decrypted, 0, body, 0, body.length);
        try (YSMByteBuf in = new YSMByteBuf(Unpooled.wrappedBuffer(body))) {
            in.skipGarbageHeader();
            if (in.readByte() != 0x02) {
                return;
            }
        }
        state.step = 2;
        sendPacket03(player, state);
    }

    private static void handlePacket04(EntityPlayerMP player, PlayerSyncState17 state, byte[] payload) throws Exception {
        byte[] decrypted = YsmCrypt.decrypt(payload, state.key1);
        List<ModelHash17> requested = new ArrayList<>();
        try (YSMByteBuf in = new YSMByteBuf(Unpooled.wrappedBuffer(decrypted))) {
            in.skipGarbageHeader();
            if (in.readByte() != 0x04) {
                return;
            }
            int count = in.readVarInt();
            for (int i = 0; i < count; i++) {
                requested.add(new ModelHash17(in.readVarLong(), in.readVarLong()));
            }
        }
        state.step = 3;
        sendPacket05(player, state, requested);
    }

    private static void sendPacket03(EntityPlayerMP player, PlayerSyncState17 state) throws Exception {
        try (YSMByteBuf out = new YSMByteBuf(Unpooled.buffer())) {
            writeGarbageHeader(out);
            out.writeVarInt(3);
            out.writeVarLong(getServerCacheFolderHash());
            out.getRawBuf().writeBytes(ServerModelManager.OPEN_YSM_SERVER_KEY);
            out.getRawBuf().writeBytes(state.clientKey);
            out.writeVarInt(state.allowedModels.size());
            for (ServerModelData17 model : state.allowedModels) {
                out.writeVarLong(model.getHash().getHash1());
                out.writeVarLong(model.getHash().getHash2());
                out.writeString(model.getModelId());
                out.writeVarInt(model.isCustomSkinModel() ? 1 : 0);
                out.writeVarInt(model.getFormat());
            }
            out.writeVarInt(0);
            YsmCrypt.EncryptedPacket packet = YsmCrypt.encrypt(out.toArray(), state.clientNextKey, false);
            sendPayload(player, packet.data());
        }
    }

    private static void sendPacket05(EntityPlayerMP player, PlayerSyncState17 state, List<ModelHash17> requested) {
        ThreadTools.THREAD_POOL.submit(() -> {
            try {
                for (ModelHash17 hash : requested) {
                    java.nio.file.Path file = ServerModelManager.CACHE_SERVER.resolve(hash.toServerCacheFileName());
                    if (!java.nio.file.Files.isRegularFile(file)) {
                        continue;
                    }
                    byte[] fileData = java.nio.file.Files.readAllBytes(file);
                    int offset = 0;
                    while (offset < fileData.length && !isTimedOut(state)) {
                        int length = Math.min(CHUNK_SIZE, fileData.length - offset);
                        try (YSMByteBuf out = new YSMByteBuf(Unpooled.buffer())) {
                            writeGarbageHeader(out);
                            out.writeVarInt(5);
                            out.writeVarLong(hash.getHash1());
                            out.writeVarLong(hash.getHash2());
                            out.writeVarInt(fileData.length);
                            out.writeVarInt(offset);
                            out.writeVarInt(length);
                            out.getRawBuf().writeBytes(fileData, offset, length);
                            YsmCrypt.EncryptedPacket packet = YsmCrypt.encrypt(out.toArray(), state.key1, false);
                            sendPayload(player, packet.data());
                            sleepForBandwidth(length);
                        }
                        offset += length;
                    }
                }
            } catch (Exception e) {
                ysmu.LOG.warn("Failed to send YSM sync17 chunks to " + player.getCommandSenderName(), e);
                clearPlayer(player);
                requestLegacySync(player);
            }
        });
    }

    private static void sendPayload(EntityPlayerMP player, byte[] data) {
        NetworkHandler.sendToClientPlayer(new S2CModelSyncPayload17(data), player);
    }

    private static void writeGarbageHeader(YSMByteBuf out) {
        int length = 16 + RANDOM.nextInt(48);
        byte[] garbage = new byte[length];
        RANDOM.nextBytes(garbage);
        out.writeGarbageHeader(length, garbage);
    }

    private static boolean isTimedOut(PlayerSyncState17 state) {
        return System.currentTimeMillis() - state.lastTouchedMillis > Config.PLAYER_SYNC_TIMEOUT * 1000L;
    }

    private static void sleepForBandwidth(int bytes) throws InterruptedException {
        int mbps = Math.max(1, Config.BANDWIDTH_LIMIT);
        long millis = Math.max(0L, (long) Math.ceil(bytes * 1000.0D / (mbps * 1024D * 1024D)));
        if (Config.LOW_BANDWIDTH_USAGE) {
            millis = Math.max(millis, 15L);
        }
        if (millis > 0L) {
            Thread.sleep(millis);
        }
    }

    private static long getServerCacheFolderHash() {
        byte[] key = ServerModelManager.OPEN_YSM_SERVER_KEY;
        if (key == null || key.length < 8) {
            return 0L;
        }
        return ByteBuffer.wrap(key, 0, 8).order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong();
    }

    private static void ensureStableModelHash(String modelId, RawYsmModel raw, ModelData data) throws Exception {
        if (raw.properties.sha256 != null && !raw.properties.sha256.isEmpty()) {
            return;
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(modelId.getBytes(StandardCharsets.UTF_8));
        if (data != null) {
            updateDigest(digest, data.getModel());
            updateDigest(digest, data.getTexture());
            updateDigest(digest, data.getAnimation());
        }
        raw.properties.sha256 = "ysmu-sync17-" + toHex(digest.digest());
    }

    private static void fillStableClientKey(byte[] output, java.util.UUID playerId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(ServerModelManager.OPEN_YSM_SERVER_KEY == null ? new byte[0] : ServerModelManager.OPEN_YSM_SERVER_KEY);
            digest.update(longBytes(playerId.getMostSignificantBits()));
            digest.update(longBytes(playerId.getLeastSignificantBits()));
            byte[] first = digest.digest();
            digest.reset();
            digest.update(first);
            digest.update("ysmu-sync17-client-key".getBytes(StandardCharsets.UTF_8));
            byte[] second = digest.digest();
            System.arraycopy(first, 0, output, 0, first.length);
            System.arraycopy(second, 0, output, first.length, output.length - first.length);
        } catch (Exception e) {
            RANDOM.nextBytes(output);
        }
    }

    private static byte[] longBytes(long value) {
        return ByteBuffer.allocate(8).order(java.nio.ByteOrder.LITTLE_ENDIAN).putLong(value).array();
    }

    private static void updateDigest(MessageDigest digest, Map<String, byte[]> values) throws Exception {
        List<String> keys = new ArrayList<>(values.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            digest.update(key.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(values.get(key));
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b & 0xFF));
        }
        return builder.toString();
    }
}
