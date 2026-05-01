package com.fox.ysmu.eep; // 建议放在一个新的包，例如 eep

import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import com.google.common.collect.Sets;

import cpw.mods.fml.common.FMLLog;

public class ExtendedAuthModels implements IExtendedEntityProperties {

    // 1. 唯一的标识符，用于注册和获取 EEP
    public final static String EXT_PROP_NAME = "ysmu_AuthModels";

    // 2. 将原 AuthModelsCapability 的字段和方法直接移到这里
    private Set<ResourceLocation> authModels = Sets.newHashSet();

    public void addModel(ResourceLocation modelId) {
        authModels.add(modelId);
    }

    public void copyFrom(ExtendedAuthModels source) {
        this.authModels = Sets.newHashSet(source.authModels); // 创建一个新的Set以避免引用问题
    }

    public void removeModel(ResourceLocation modelId) {
        authModels.remove(modelId);
    }

    public boolean containModel(ResourceLocation modelId) {
        return authModels.contains(modelId);
    }

    public Set<ResourceLocation> getAuthModels() {
        return authModels;
    }

    public void setAuthModels(Set<ResourceLocation> authModels) {
        this.authModels = authModels;
    }

    public void clear() {
        authModels.clear();
    }

    // 3. 静态辅助方法，用于方便地注册和获取
    /**
     * 将 EEP 注册到玩家身上
     */
    public static void register(EntityPlayer player) {
        player.registerExtendedProperties(EXT_PROP_NAME, new ExtendedAuthModels());
    }

    /**
     * 从玩家身上获取 EEP 实例
     * 这个方法替代了高版本中的 getCapability()
     */
    public static ExtendedAuthModels get(EntityPlayer player) {
        return (ExtendedAuthModels) player.getExtendedProperties(EXT_PROP_NAME);
    }

    // 4. 实现 IExtendedEntityProperties 接口的必要方法

    /**
     * 将数据保存到 NBT
     * 对应原 AuthModelsCapability.serializeNBT()
     */
    @Override
    public void saveNBTData(NBTTagCompound compound) {
        // 创建一个新的 NBTTagList 来存储我们的模型ID
        NBTTagList listTag = new NBTTagList();
        for (ResourceLocation modelId : authModels) {
            listTag.appendTag(new NBTTagString(modelId.toString()));
        }
        // 将这个列表以我们的唯一标识符为键，存入 compound
        compound.setTag(EXT_PROP_NAME, listTag);
    }

    /**
     * 从 NBT 读取数据
     * 对应原 AuthModelsCapability.deserializeNBT()
     */
    @Override
    public void loadNBTData(NBTTagCompound compound) {
        // 检查 compound 中是否存在我们的数据
        if (compound.hasKey(EXT_PROP_NAME)) {
            NBTTagList listTag = (NBTTagList) compound.getTag(EXT_PROP_NAME);
            this.authModels.clear();
            for (int i = 0; i < listTag.tagCount(); i++) {
                String modelIdStr = listTag.getStringTagAt(i);
                try {
                    authModels.add(new ResourceLocation(modelIdStr));
                } catch (Exception e) {
                    FMLLog.warning("[ysmu] Failed to load ResourceLocation from NBT: " + modelIdStr);
                }
            }
        }
    }

    @Override
    public void init(Entity entity, World world) {
        // 这个方法在 EEP 初始化时调用，这里我们不需要做什么
    }
}
