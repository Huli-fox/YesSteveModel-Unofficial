package com.fox.ysmu.network.message.sync17;

import net.minecraft.entity.player.EntityPlayerMP;

import com.fox.ysmu.network.sync.ModelSyncServer17;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2SVersionCheck17 implements IMessage {

    private int protocolVersion;
    private boolean supported;

    public C2SVersionCheck17() {}

    public C2SVersionCheck17(int protocolVersion, boolean supported) {
        this.protocolVersion = protocolVersion;
        this.supported = supported;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.protocolVersion = buf.readInt();
        this.supported = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.protocolVersion);
        buf.writeBoolean(this.supported);
    }

    public static class Handler implements IMessageHandler<C2SVersionCheck17, IMessage> {

        @Override
        public IMessage onMessage(C2SVersionCheck17 message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player != null) {
                ModelSyncServer17.handleVersionCheck(player, message.protocolVersion, message.supported);
            }
            return null;
        }
    }
}
