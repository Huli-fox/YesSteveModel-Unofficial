package com.fox.ysmu.eep; // 放在 eep 包下

import com.google.common.collect.Sets;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import java.util.Set;

public class ExtendedStarModels implements IExtendedEntityProperties {

    // 1. 唯一的标识符
    public final static String EXT_PROP_NAME = "ysmu_StarModels";

    // 2. 将原 StarModelsCapability 的字段和方法直接移到这里
    private Set<ResourceLocation> starModels = Sets.newHashSet();

    public void addModel(ResourceLocation modelId) {
        starModels.add(modelId);
    }

    public void copyFrom(ExtendedStarModels source) {
        // 创建一个新的 Set 副本，而不是直接引用，这很重要！
        this.starModels = Sets.newHashSet(source.starModels);
    }

    public void removeModel(ResourceLocation modelId) {
        starModels.remove(modelId);
    }

    public boolean containModel(ResourceLocation modelId) {
        return starModels.contains(modelId);
    }

    public Set<ResourceLocation> getStarModels() {
        return starModels;
    }

    public void setStarModels(Set<ResourceLocation> starModels) {
        this.starModels = starModels;
    }

    public void clear() {
        starModels.clear();
    }

    // 3. 静态辅助方法
    /**
     * 将 EEP 注册到玩家身上
     */
    public static void register(EntityPlayer player) {
        player.registerExtendedProperties(EXT_PROP_NAME, new ExtendedStarModels());
    }

    /**
     * 从玩家身上获取 EEP 实例
     */
    public static ExtendedStarModels get(EntityPlayer player) {
        return (ExtendedStarModels) player.getExtendedProperties(EXT_PROP_NAME);
    }

    // 4. 实现 IExtendedEntityProperties 接口的方法

    /**
     * 将数据保存到 NBT
     */
    @Override
    public void saveNBTData(NBTTagCompound compound) {
        NBTTagList listTag = new NBTTagList();
        for (ResourceLocation modelId : starModels) {
            listTag.appendTag(new NBTTagString(modelId.toString()));
        }
        compound.setTag(EXT_PROP_NAME, listTag);
    }

    /**
     * 从 NBT 读取数据
     */
    @Override
    public void loadNBTData(NBTTagCompound compound) {
        if (compound.hasKey(EXT_PROP_NAME)) {
            NBTTagList listTag = (NBTTagList) compound.getTag(EXT_PROP_NAME);
            this.starModels.clear();
            for (int i = 0; i < listTag.tagCount(); i++) {
                String modelIdStr = listTag.getStringTagAt(i);
                try {
                    starModels.add(new ResourceLocation(modelIdStr));
                } catch (Exception e) {
                    FMLLog.warning("[ysmu] Failed to load Starred Model ResourceLocation from NBT: " + modelIdStr);
                }
            }
        }
    }

    @Override
    public void init(Entity entity, World world) {
        // 初始化时调用，此处无需操作
    }
}
