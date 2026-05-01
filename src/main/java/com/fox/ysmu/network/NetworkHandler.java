package com.fox.ysmu.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.fox.ysmu.network.message.*;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class NetworkHandler {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("ysmu_network");

    // Packet ids are part of the wire protocol. Add new ids; do not renumber existing ones.
    private static final int SERVERBOUND_SYNC_MODEL_FILES = 0;
    private static final int SERVERBOUND_SET_MODEL_AND_TEXTURE = 5;
    private static final int SERVERBOUND_SET_PLAY_ANIMATION = 7;
    private static final int SERVERBOUND_SET_STAR_MODEL = 9;

    private static final int CLIENTBOUND_SEND_MODEL_FILE = 1;
    private static final int CLIENTBOUND_REQUEST_SYNC_MODEL = 2;
    private static final int CLIENTBOUND_REQUEST_LOAD_MODEL = 3;
    private static final int CLIENTBOUND_SYNC_MODEL_INFO = 4;
    private static final int CLIENTBOUND_SYNC_STAR_MODELS = 8;
    private static final int CLIENTBOUND_REQUEST_SERVER_MODEL_INFO = 10;
    private static final int CLIENTBOUND_SYNC_PLAYER_MOTION_STATE = 11;
    private static final int CLIENTBOUND_COMPLETE_FEEDBACK = 12;

    public static final int OPEN_NPC_MODEL_GUI = 93;
    public static final int SET_NPC_MODEL_ID = 94;
    public static final int SYNC_NPC_DATA = 95;
    public static final int UPDATE_NPC_DATA = 96;

    public static void init() {
        registerServerboundMessages();
        registerClientboundMessages();
        initBukkit();
    }

    private static void registerServerboundMessages() {
        CHANNEL.registerMessage(
            SyncModelFiles.Handler.class,
            SyncModelFiles.class,
            SERVERBOUND_SYNC_MODEL_FILES,
            Side.SERVER);
        CHANNEL.registerMessage(
            SetModelAndTexture.Handler.class,
            SetModelAndTexture.class,
            SERVERBOUND_SET_MODEL_AND_TEXTURE,
            Side.SERVER);
        CHANNEL.registerMessage(
            SetPlayAnimation.Handler.class,
            SetPlayAnimation.class,
            SERVERBOUND_SET_PLAY_ANIMATION,
            Side.SERVER);
        CHANNEL.registerMessage(
            SetStarModel.Handler.class,
            SetStarModel.class,
            SERVERBOUND_SET_STAR_MODEL,
            Side.SERVER);
    }

    private static void registerClientboundMessages() {
        CHANNEL.registerMessage(
            SendModelFile.Handler.class,
            SendModelFile.class,
            CLIENTBOUND_SEND_MODEL_FILE,
            Side.CLIENT);
        CHANNEL.registerMessage(
            RequestSyncModel.Handler.class,
            RequestSyncModel.class,
            CLIENTBOUND_REQUEST_SYNC_MODEL,
            Side.CLIENT);
        CHANNEL.registerMessage(
            RequestLoadModel.Handler.class,
            RequestLoadModel.class,
            CLIENTBOUND_REQUEST_LOAD_MODEL,
            Side.CLIENT);
        CHANNEL.registerMessage(
            SyncModelInfo.Handler.class,
            SyncModelInfo.class,
            CLIENTBOUND_SYNC_MODEL_INFO,
            Side.CLIENT);
        CHANNEL.registerMessage(
            SyncStarModels.Handler.class,
            SyncStarModels.class,
            CLIENTBOUND_SYNC_STAR_MODELS,
            Side.CLIENT);
        CHANNEL.registerMessage(
            RequestServerModelInfo.Handler.class,
            RequestServerModelInfo.class,
            CLIENTBOUND_REQUEST_SERVER_MODEL_INFO,
            Side.CLIENT);
        CHANNEL.registerMessage(
            SyncPlayerMotionState.Handler.class,
            SyncPlayerMotionState.class,
            CLIENTBOUND_SYNC_PLAYER_MOTION_STATE,
            Side.CLIENT);
        CHANNEL.registerMessage(
            CompleteFeedback.Handler.class,
            CompleteFeedback.class,
            CLIENTBOUND_COMPLETE_FEEDBACK,
            Side.CLIENT);
    }

    private static void initBukkit() {
        CHANNEL.registerMessage(
            OpenModelGuiMessage.Handler.class,
            OpenModelGuiMessage.class,
            OPEN_NPC_MODEL_GUI,
            Side.CLIENT);
        CHANNEL.registerMessage(
            SetNpcModelAndTexture.Handler.class,
            SetNpcModelAndTexture.class,
            SET_NPC_MODEL_ID,
            Side.SERVER);
        CHANNEL.registerMessage(
            SyncNpcDataMessage.Handler.class,
            SyncNpcDataMessage.class,
            SYNC_NPC_DATA,
            Side.CLIENT);
        CHANNEL.registerMessage(
            UpdateNpcDataMessage.Handler.class,
            UpdateNpcDataMessage.class,
            UPDATE_NPC_DATA,
            Side.CLIENT);
    }

    public static void sendToClientPlayer(IMessage message, EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            CHANNEL.sendTo(message, (EntityPlayerMP) player);
        }
    }
}
