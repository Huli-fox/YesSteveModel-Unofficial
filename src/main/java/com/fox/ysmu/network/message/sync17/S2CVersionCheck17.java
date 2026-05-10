package com.fox.ysmu.network.message;

import com.fox.ysmu.client.sync.OpenYsmModelSyncClient;
import com.fox.ysmu.Config;
import com.fox.ysmu.network.NetworkHandler;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public class S2CVersionCheck17 implements IMessage {

    private String version = "";

    public S2CVersionCheck17() {}

    public S2CVersionCheck17(String version) {
        this.version = version == null ? "" : version;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.version = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.version);
    }

    public String getVersion() {
        return version;
    }

    public static class Handler implements IMessageHandler<S2CVersionCheck17, IMessage> {

        @Override
        public IMessage onMessage(S2CVersionCheck17 message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT && Config.ENABLE_OPEN_YSM_SYNC_PROTOCOL) {
                OpenYsmModelSyncClient.resetConnectionState();
                NetworkHandler.CHANNEL.sendToServer(new C2SVersionCheck17(NetworkHandler.PROTOCOL_VERSION));
            }
            return null;
        }
    }
}
