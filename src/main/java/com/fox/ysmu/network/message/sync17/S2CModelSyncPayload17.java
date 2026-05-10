package com.fox.ysmu.network.message;

import com.fox.ysmu.client.sync.OpenYsmModelSyncClient;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public class S2CModelSyncPayload17 implements IMessage {

    private byte[] data = new byte[0];

    public S2CModelSyncPayload17() {}

    public S2CModelSyncPayload17(byte[] data) {
        this.data = data == null ? new byte[0] : data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.data = C2SModelSyncPayload17.readByteArray(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        C2SModelSyncPayload17.writeByteArray(buf, this.data);
    }

    public byte[] getData() {
        return data;
    }

    public static class Handler implements IMessageHandler<S2CModelSyncPayload17, IMessage> {

        @Override
        public IMessage onMessage(S2CModelSyncPayload17 message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                OpenYsmModelSyncClient.handlePayload(message.data);
            }
            return null;
        }
    }
}
