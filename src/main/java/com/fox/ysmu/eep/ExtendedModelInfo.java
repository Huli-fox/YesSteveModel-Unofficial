package com.fox.ysmu.eep; // 建议放在 eep 包下

import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.SyncModelInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import com.fox.ysmu.Config;
import com.fox.ysmu.ysmu;

public class ExtendedModelInfo implements IExtendedEntityProperties {

    // 1. 唯一的标识符
    public final static String EXT_PROP_NAME = "ysmu_ModelInfo";

    // 用于网络同步和服务器端逻辑
    private final EntityPlayer player;

    // 2. 将原 ModelInfoCapability 的字段和方法直接移到这里
    private ResourceLocation modelId = new ResourceLocation(ysmu.MODID, Config.DEFAULT_MODEL_ID);
    private ResourceLocation selectTexture = new ResourceLocation(
        ysmu.MODID,
        Config.DEFAULT_MODEL_ID + "/" + Config.DEFAULT_MODEL_TEXTURE);
    private String animation = "idle";
    private boolean playAnimation = false;
    private boolean dirty; // dirty 标志可以保留，用于客户端渲染逻辑判断是否需要更新

    public ExtendedModelInfo(EntityPlayer player) {
        this.player = player;
    }

    public void setModelAndTexture(ResourceLocation modelId, ResourceLocation selectTexture) {
        this.modelId = modelId;
        this.selectTexture = selectTexture;
        markDirty();
    }

    public void copyFrom(ExtendedModelInfo source) {
        this.modelId = source.modelId;
        this.selectTexture = source.selectTexture;
        this.animation = source.animation;
        this.playAnimation = source.playAnimation;
        markDirty();
    }

    public ResourceLocation getModelId() {
        return modelId;
    }

    public ResourceLocation getSelectTexture() {
        return selectTexture;
    }

    public void setSelectTexture(ResourceLocation selectTexture) {
        this.selectTexture = selectTexture;
        markDirty();
    }

    public void playAnimation(String animation) {
        this.animation = animation;
        this.playAnimation = true;
        markDirty();
    }

    public void stopAnimation() {
        this.playAnimation = false;
        markDirty();
    }

    public String getAnimation() {
        return animation;
    }

    public boolean isPlayAnimation() {
        return playAnimation;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    // 3. 静态辅助方法
    /**
     * 将 EEP 注册到玩家身上
     */
    public static void register(EntityPlayer player) {
        player.registerExtendedProperties(EXT_PROP_NAME, new ExtendedModelInfo(player));
    }

    /**
     * 从玩家身上获取 EEP 实例
     */
    public static ExtendedModelInfo get(EntityPlayer player) {
        return (ExtendedModelInfo) player.getExtendedProperties(EXT_PROP_NAME);
    }

    // 4. 实现 IExtendedEntityProperties 接口的方法

    /**
     * 将数据保存到 NBT
     * 这个方法会把所有字段打包到一个独立的 NBTTagCompound 中，避免命名冲突
     */
    @Override
    public void saveNBTData(NBTTagCompound compound) {
        NBTTagCompound properties = new NBTTagCompound();
        properties.setString("model_id", this.modelId.toString());
        properties.setString("select_texture", this.selectTexture.toString());
        properties.setString("animation", this.animation);
        properties.setBoolean("play_animation", this.playAnimation);

        compound.setTag(EXT_PROP_NAME, properties);
    }

    /**
     * 从 NBT 读取数据
     */
    @Override
    public void loadNBTData(NBTTagCompound compound) {
        if (compound.hasKey(EXT_PROP_NAME)) {
            NBTTagCompound properties = compound.getCompoundTag(EXT_PROP_NAME);
            this.modelId = new ResourceLocation(properties.getString("model_id"));
            this.selectTexture = new ResourceLocation(properties.getString("select_texture"));
            this.animation = properties.getString("animation");
            this.playAnimation = properties.getBoolean("play_animation");
        }
    }

    @Override
    public void init(Entity entity, World world) {
        // 初始化时调用
    }
}
