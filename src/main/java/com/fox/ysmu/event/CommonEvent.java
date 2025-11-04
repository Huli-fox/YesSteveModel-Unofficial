package com.fox.ysmu.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityEvent;

import com.fox.ysmu.eep.ExtendedAuthModels;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.eep.ExtendedStarModels;
import com.fox.ysmu.model.ServerModelManager;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@EventBusSubscriber
public final class CommonEvent {

    @SubscribeEvent
    public static void onPlayerLoggedIn(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        // 调用发送模型同步请求的方法
        if (event.player != null) {
            ServerModelManager.sendRequestSyncModelMessage(event.player);
        }
    }

    @SubscribeEvent
    public static void onEntityConstructing(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.entity;
            // 注册 AuthModels
            if (ExtendedAuthModels.get(player) == null) {
                ExtendedAuthModels.register(player);
            }
            // 注册 ModelInfo
            if (ExtendedModelInfo.get(player) == null) {
                ExtendedModelInfo.register(player);
            }
            // 2. 注册 StarModels
            if (ExtendedStarModels.get(player) == null) {
                ExtendedStarModels.register(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.wasDeath) {
            // 复制 AuthModels 数据
            ExtendedAuthModels oldAuthProps = ExtendedAuthModels.get(event.original);
            ExtendedAuthModels newAuthProps = ExtendedAuthModels.get(event.entityPlayer);
            if (oldAuthProps != null && newAuthProps != null) {
                newAuthProps.copyFrom(oldAuthProps);
            }

            // 复制 ModelInfo 数据
            ExtendedModelInfo oldModelProps = ExtendedModelInfo.get(event.original);
            ExtendedModelInfo newModelProps = ExtendedModelInfo.get(event.entityPlayer);
            if (oldModelProps != null && newModelProps != null) {
                newModelProps.copyFrom(oldModelProps);
            }

            // 3. 复制 StarModels 数据
            ExtendedStarModels oldStarProps = ExtendedStarModels.get(event.original);
            ExtendedStarModels newStarProps = ExtendedStarModels.get(event.entityPlayer);
            if (oldStarProps != null && newStarProps != null) {
                newStarProps.copyFrom(oldStarProps);
            }
        }
    }
}
