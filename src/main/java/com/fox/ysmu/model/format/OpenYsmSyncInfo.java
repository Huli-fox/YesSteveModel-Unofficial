package com.fox.ysmu.model.format;

public final class OpenYsmSyncInfo {

    private final String modelId;
    private final String cacheFileName;
    private final long hash1;
    private final long hash2;
    private final int format;
    private final boolean customSkinModel;

    public OpenYsmSyncInfo(String modelId, String cacheFileName, long hash1, long hash2, int format,
        boolean customSkinModel) {
        this.modelId = modelId;
        this.cacheFileName = cacheFileName;
        this.hash1 = hash1;
        this.hash2 = hash2;
        this.format = format;
        this.customSkinModel = customSkinModel;
    }

    public String getModelId() {
        return modelId;
    }

    public String getCacheFileName() {
        return cacheFileName;
    }

    public long getHash1() {
        return hash1;
    }

    public long getHash2() {
        return hash2;
    }

    public int getFormat() {
        return format;
    }

    public boolean isCustomSkinModel() {
        return customSkinModel;
    }

    public boolean matches(long otherHash1, long otherHash2) {
        return hash1 == otherHash1 && hash2 == otherHash2;
    }
}
