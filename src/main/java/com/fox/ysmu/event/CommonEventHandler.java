package com.fox.ysmu.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import com.fox.ysmu.data.PlayerMotionState;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.eep.ExtendedStarModels;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.SyncModelInfo;
import com.fox.ysmu.network.message.SyncPlayerMotionState;
import com.fox.ysmu.network.message.SyncStarModels;
import com.fox.ysmu.network.sync.ModelSyncServer17;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@EventBusSubscriber
public class CommonEventHandler {

    private static final Map<UUID, Byte> LAST_MOTION_STATES = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerLoggedIn(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player != null) {
            requestModelSync(event.player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            LAST_MOTION_STATES.remove(event.player.getUniqueID());
            ModelSyncServer17.clearPlayer(event.player);
        }
    }

    @SubscribeEvent
    public static void onEntityConstructing(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof EntityPlayer player) {
            registerPlayerProperties(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.wasDeath) { // TODO 跨维度？
            copyPlayerProperties(event.original, event.entityPlayer);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.target instanceof EntityPlayer trackPlayer) {
            syncTrackedPlayerModelInfo(event.entityPlayer, trackPlayer);
            syncTrackedPlayerMotionState(event.entityPlayer, trackPlayer);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityPlayer player) {
            syncJoinedPlayerState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (shouldHandleServerEndPlayerTick(event)) {
            syncDirtyMotionState(event.player);
            broadcastDirtyModelInfo(event.player);
        }
    }

    private static void requestModelSync(EntityPlayer player) {
        ServerModelManager.sendRequestSyncModelMessage(player);
    }

    private static void registerPlayerProperties(EntityPlayer player) {
        if (ExtendedModelInfo.get(player) == null) {
            ExtendedModelInfo.register(player);
        }
        if (ExtendedStarModels.get(player) == null) {
            ExtendedStarModels.register(player);
        }
    }

    private static void syncTrackedPlayerMotionState(EntityPlayer trackingPlayer, EntityPlayer trackedPlayer) {
        if (!trackedPlayer.worldObj.isRemote) {
            NetworkHandler.sendToClientPlayer(
                createMotionStateMessage(trackedPlayer, PlayerMotionState.pack(trackedPlayer)),
                trackingPlayer);
        }
    }

    private static void copyPlayerProperties(EntityPlayer oldPlayer, EntityPlayer newPlayer) {
        ExtendedModelInfo oldModelProps = ExtendedModelInfo.get(oldPlayer);
        ExtendedModelInfo newModelProps = ExtendedModelInfo.get(newPlayer);
        if (oldModelProps != null && newModelProps != null) {
            newModelProps.copyFrom(oldModelProps);
        }

        ExtendedStarModels oldStarProps = ExtendedStarModels.get(oldPlayer);
        ExtendedStarModels newStarProps = ExtendedStarModels.get(newPlayer);
        if (oldStarProps != null && newStarProps != null) {
            newStarProps.copyFrom(oldStarProps);
        }
    }

    private static void syncTrackedPlayerModelInfo(EntityPlayer trackingPlayer, EntityPlayer trackedPlayer) {
        ExtendedModelInfo modelInfo = ExtendedModelInfo.get(trackedPlayer);
        if (modelInfo != null) {
            SyncModelInfo syncMsg = new SyncModelInfo(trackedPlayer.getEntityId(), modelInfo);
            NetworkHandler.sendToClientPlayer(syncMsg, trackingPlayer);
        }
    }

    private static void syncJoinedPlayerState(EntityPlayer player) {
        ExtendedModelInfo modelInfo = ExtendedModelInfo.get(player);
        if (modelInfo != null) {
            if (player instanceof EntityPlayerMP serverPlayer) {
                NetworkHandler.sendToClientPlayer(new SyncModelInfo(serverPlayer.getEntityId(), modelInfo), serverPlayer);
            } else {
                modelInfo.markDirty();
            }
        }
        syncStarModels(player);
    }

    private static void syncStarModels(EntityPlayer player) {
        ExtendedStarModels starModels = ExtendedStarModels.get(player);
        if (starModels != null && player instanceof EntityPlayerMP serverPlayer) {
            NetworkHandler.sendToClientPlayer(new SyncStarModels(starModels.getStarModels()), serverPlayer);
        }
    }

    private static boolean shouldHandleServerEndPlayerTick(TickEvent.PlayerTickEvent event) {
        return event.player != null && event.side.isServer() && event.phase == TickEvent.Phase.END;
    }

    private static void broadcastDirtyModelInfo(EntityPlayer player) {
        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep != null && eep.isDirty()) {
            SyncModelInfo syncMsg = new SyncModelInfo(player.getEntityId(), eep);
            NetworkHandler.CHANNEL.sendToAllAround(syncMsg, getPlayerTrackingPoint(player));
            eep.setDirty(false);
        }
    }

    private static NetworkRegistry.TargetPoint getPlayerTrackingPoint(EntityPlayer player) {
        return new NetworkRegistry.TargetPoint(
            player.dimension,
            player.posX,
            player.posY,
            player.posZ,
            64.0D // 64个方块的范围，这是一个常用值
        );
    }

    private static void syncDirtyMotionState(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        byte newState = PlayerMotionState.pack(player);
        Byte oldState = LAST_MOTION_STATES.get(playerId);
        if (oldState == null || oldState.byteValue() != newState) {
            LAST_MOTION_STATES.put(playerId, newState);
            NetworkHandler.CHANNEL.sendToDimension(createMotionStateMessage(player, newState), player.dimension);
        }
    }

    private static SyncPlayerMotionState createMotionStateMessage(EntityPlayer player, byte motionState) {
        return new SyncPlayerMotionState(player.getUniqueID(), motionState);
    }
}
