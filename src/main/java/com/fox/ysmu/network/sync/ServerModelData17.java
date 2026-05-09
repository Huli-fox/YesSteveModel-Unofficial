package com.fox.ysmu.network.sync;

import java.util.Collections;
import java.util.Set;

import com.fox.ysmu.model.resource.pojo.RawYsmModel;

public final class ServerModelData17 {

    private final String modelId;
    private final String modelHash;
    private final ModelHash17 hash;
    private final int format;
    private final boolean customSkinModel;
    private final Set<String> textures;
    private final RawYsmModel rawModel;

    public ServerModelData17(String modelId, String modelHash, ModelHash17 hash, int format, boolean customSkinModel,
        Set<String> textures, RawYsmModel rawModel) {
        this.modelId = modelId;
        this.modelHash = modelHash;
        this.hash = hash;
        this.format = format;
        this.customSkinModel = customSkinModel;
        this.textures = textures == null ? Collections.emptySet() : Collections.unmodifiableSet(textures);
        this.rawModel = rawModel;
    }

    public String getModelId() {
        return modelId;
    }

    public String getModelHash() {
        return modelHash;
    }

    public ModelHash17 getHash() {
        return hash;
    }

    public int getFormat() {
        return format;
    }

    public boolean isCustomSkinModel() {
        return customSkinModel;
    }

    public Set<String> getTextures() {
        return textures;
    }

    public RawYsmModel getRawModel() {
        return rawModel;
    }
}
