package com.fox.ysmu.network.message.sync17;

import com.fox.ysmu.network.sync.ModelSyncClient17;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public class S2CModelSyncPayload17 implements IMessage {

    private byte[] payload;

    public S2CModelSyncPayload17() {}

    public S2CModelSyncPayload17(byte[] payload) {
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

    public static class Handler implements IMessageHandler<S2CModelSyncPayload17, IMessage> {

        @Override
        public IMessage onMessage(S2CModelSyncPayload17 message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                ModelSyncClient17.handlePayload(message.payload);
            }
            return null;
        }
    }
}
