package com.fox.ysmu.network.message;

import net.minecraft.entity.player.EntityPlayerMP;

import com.fox.ysmu.Config;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.sync.OpenYsmModelSyncServer;
import com.fox.ysmu.ysmu;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2SVersionCheck17 implements IMessage {

    private String version = "";

    public C2SVersionCheck17() {}

    public C2SVersionCheck17(String version) {
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

    public static class Handler implements IMessageHandler<C2SVersionCheck17, IMessage> {

        @Override
        public IMessage onMessage(C2SVersionCheck17 message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (sender == null || !Config.ENABLE_OPEN_YSM_SYNC_PROTOCOL) {
                return null;
            }
            if (!NetworkHandler.PROTOCOL_VERSION.equals(message.version)) {
                ysmu.LOG.warn(
                    "Skipping OpenYSM model sync for {} because protocol versions differ: client={}, server={}",
                    sender.getCommandSenderName(),
                    message.version,
                    NetworkHandler.PROTOCOL_VERSION);
                return null;
            }
            OpenYsmModelSyncServer.startSync(sender);
            return null;
        }
    }
}
