package com.fox.ysmu.network.message.sync17;

import net.minecraft.entity.player.EntityPlayerMP;

import com.fox.ysmu.network.sync.ModelSyncServer17;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2SModelSyncPayload17 implements IMessage {

    private byte[] payload;

    public C2SModelSyncPayload17() {}

    public C2SModelSyncPayload17(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.payload = new byte[buf.readInt()];
        buf.readBytes(this.payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.payload.length);
        buf.writeBytes(this.payload);
    }

    public static class Handler implements IMessageHandler<C2SModelSyncPayload17, IMessage> {

        @Override
        public IMessage onMessage(C2SModelSyncPayload17 message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player != null) {
                ModelSyncServer17.handlePayload(player, message.payload);
            }
            return null;
        }
    }
}
