package com.fox.ysmu.data;

import java.util.Map;

import com.fox.ysmu.model.format.ServerModelInfo;
import com.fox.ysmu.model.format.Type;

public final class ModelData {

    private final String modelId;
    private final Map<String, byte[]> model;
    private final Map<String, byte[]> texture;
    private final Map<String, byte[]> animation;
    private final ServerModelInfo info;

    public ModelData(String modelId, boolean isAuth, Type type, Map<String, byte[]> geo, Map<String, byte[]> texture,
        Map<String, byte[]> animation) {
        this.modelId = modelId;
        this.model = geo;
        this.texture = texture;
        this.animation = animation;
        this.info = new ServerModelInfo(texture.keySet(), isAuth, type);
    }

    public String getModelId() {
        return modelId;
    }

    public Map<String, byte[]> getModel() {
        return model;
    }

    public Map<String, byte[]> getTexture() {
        return texture;
    }

    public Map<String, byte[]> getAnimation() {
        return animation;
    }

    public boolean isAuth() {
        return this.info.isNeedAuth();
    }

    public void setMd5(String md5) {
        this.info.setMd5(md5);
    }

    public ServerModelInfo getInfo() {
        return info;
    }
}
