package com.fox.ysmu.network;

// import com.fox.ysmu.bukkit.message.OpenModelGuiMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.fox.ysmu.network.message.*;
import com.fox.ysmu.network.message.SetNpcModelAndTexture;
import com.fox.ysmu.network.message.SyncNpcDataMessage;
import com.fox.ysmu.network.message.UpdateNpcDataMessage;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
// import net.minecraftforge.network.PacketDistributor;
// import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {

    private static final String VERSION = "1.0.0";
    // public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(ysmu.MODID,
    // "network"),
    // () -> VERSION, it -> it.equals(VERSION), it -> it.equals(VERSION));
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("ysmu_network");
    public static final int OPEN_NPC_MODEL_GUI = 93;
    public static final int SET_NPC_MODEL_ID = 94;
    public static final int SYNC_NPC_DATA = 95;
    public static final int UPDATE_NPC_DATA = 96;

    public static void init() {
        CHANNEL.registerMessage(SyncModelFiles.Handler.class, SyncModelFiles.class, 0, Side.SERVER);
        CHANNEL.registerMessage(SetModelAndTexture.Handler.class, SetModelAndTexture.class, 5, Side.SERVER);
        CHANNEL.registerMessage(SetPlayAnimation.Handler.class, SetPlayAnimation.class, 7, Side.SERVER);
        CHANNEL.registerMessage(SetStarModel.Handler.class, SetStarModel.class, 9, Side.SERVER);
        CHANNEL.registerMessage(UploadFile.Handler.class, UploadFile.class, 11, Side.SERVER);
        CHANNEL.registerMessage(RefreshModelManage.Handler.class, RefreshModelManage.class, 13, Side.SERVER);
        CHANNEL.registerMessage(HandleFile.Handler.class, HandleFile.class, 14, Side.SERVER);

        // Side.CLIENT
        CHANNEL.registerMessage(SendModelFile.Handler.class, SendModelFile.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(RequestSyncModel.Handler.class, RequestSyncModel.class, 2, Side.CLIENT);
        CHANNEL.registerMessage(RequestLoadModel.Handler.class, RequestLoadModel.class, 3, Side.CLIENT);
        CHANNEL.registerMessage(SyncModelInfo.Handler.class, SyncModelInfo.class, 4, Side.CLIENT);
        CHANNEL.registerMessage(SyncAuthModels.Handler.class, SyncAuthModels.class, 6, Side.CLIENT);
        CHANNEL.registerMessage(SyncStarModels.Handler.class, SyncStarModels.class, 8, Side.CLIENT);
        CHANNEL.registerMessage(RequestServerModelInfo.Handler.class, RequestServerModelInfo.class, 10, Side.CLIENT);
        CHANNEL.registerMessage(CompleteFeedback.Handler.class, CompleteFeedback.class, 12, Side.CLIENT);
        initBukkit();
    }

    private static void initBukkit() {
        // CHANNEL.registerMessage(OpenModelGuiMessage.Handler.class, OpenModelGuiMessage.class, OPEN_NPC_MODEL_GUI,
        // Side.CLIENT);
        CHANNEL.registerMessage(
            SetNpcModelAndTexture.Handler.class,
            SetNpcModelAndTexture.class,
            SET_NPC_MODEL_ID,
            Side.SERVER);
        CHANNEL.registerMessage(SyncNpcDataMessage.Handler.class, SyncNpcDataMessage.class, SYNC_NPC_DATA, Side.CLIENT);
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
