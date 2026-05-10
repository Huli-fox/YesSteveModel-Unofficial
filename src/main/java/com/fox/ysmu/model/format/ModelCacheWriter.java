package com.fox.ysmu.model.format;

import static com.fox.ysmu.model.ServerModelManager.CACHE_SERVER;
import static com.fox.ysmu.model.ServerModelManager.OPEN_YSM_SERVER_KEY;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.fox.ysmu.Config;
import com.fox.ysmu.data.EncryptTools;
import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.model.resource.YSMBinarySerializer;
import com.fox.ysmu.model.resource.pojo.RawYsmModel;
import com.fox.ysmu.util.Md5Utils;

import rip.ysm.security.YSMByteBuf;
import rip.ysm.security.YsmCrypt;

final class ModelCacheWriter {

    private static final int OPEN_YSM_SYNC_FORMAT = 32;

    private ModelCacheWriter() {}

    static ServerModelInfo write(ModelData data) throws Exception {
        byte[] dataBytes = EncryptTools.assembleEncryptModels(data);
        data.setMd5(Md5Utils.md5Hex(dataBytes).toUpperCase(Locale.US));
        FileUtils.writeByteArrayToFile(
            CACHE_SERVER.resolve(
                data.getInfo()
                    .getMd5())
                .toFile(),
            dataBytes);
        return data.getInfo();
    }

    static OpenYsmSyncInfo writeOpenYsm(RawYsmModel raw, String modelId) throws Exception {
        if (OPEN_YSM_SERVER_KEY == null || OPEN_YSM_SERVER_KEY.length != 56) {
            throw new IllegalStateException("OpenYSM server key is not initialized");
        }

        String hashSource = getHashSource(raw, modelId);
        long[] hashes = YsmCrypt.calculateModelHashes(hashSource, OPEN_YSM_SERVER_KEY);
        String cacheFileName = String.format(Locale.US, "%016x%016x", hashes[0], hashes[1]);

        byte[] clearBytes = serializeForOpenYsmSync(raw);

        byte[] cacheBytes = YsmCrypt.encryptServerCache(clearBytes, OPEN_YSM_SERVER_KEY, hashes[0], hashes[1]);
        FileUtils.writeByteArrayToFile(CACHE_SERVER.resolve(cacheFileName).toFile(), cacheBytes);
        return new OpenYsmSyncInfo(modelId, cacheFileName, hashes[0], hashes[1], OPEN_YSM_SYNC_FORMAT, false);
    }

    private static byte[] serializeForOpenYsmSync(RawYsmModel raw) {
        Map<String, RawYsmModel.RawDataFile> originalSoundFiles = raw.soundFiles;
        if (!Config.ACCEPT_SOUND_FX && raw.soundFiles != null && !raw.soundFiles.isEmpty()) {
            raw.soundFiles = new java.util.LinkedHashMap<>();
        }
        try (YSMByteBuf serialized = YSMBinarySerializer.serialize(raw, OPEN_YSM_SYNC_FORMAT, true)) {
            return serialized.toArray();
        } finally {
            raw.soundFiles = originalSoundFiles;
        }
    }

    private static String getHashSource(RawYsmModel raw, String modelId) {
        if (raw != null && raw.properties != null && raw.properties.sha256 != null
            && !raw.properties.sha256.isEmpty()) {
            return raw.properties.sha256;
        }
        if (raw != null && raw.modelId != null && !raw.modelId.isEmpty()) {
            return raw.modelId;
        }
        return modelId;
    }
}
