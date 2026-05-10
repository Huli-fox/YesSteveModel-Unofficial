package com.fox.ysmu.network.message;

import net.minecraft.entity.player.EntityPlayerMP;

import com.fox.ysmu.network.sync.OpenYsmModelSyncServer;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2SModelSyncPayload17 implements IMessage {

    private static final int MAX_SYNC_PAYLOAD_BYTES = 2 * 1024 * 1024;

    private byte[] data = new byte[0];

    public C2SModelSyncPayload17() {}

    public C2SModelSyncPayload17(byte[] data) {
        this.data = data == null ? new byte[0] : data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.data = readByteArray(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writeByteArray(buf, this.data);
    }

    public byte[] getData() {
        return data;
    }

    static byte[] readByteArray(ByteBuf buf) {
        int length = buf.readInt();
        if (length < 0 || length > MAX_SYNC_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Invalid OpenYSM sync payload length: " + length);
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }

    static void writeByteArray(ByteBuf buf, byte[] bytes) {
        byte[] safeBytes = bytes == null ? new byte[0] : bytes;
        if (safeBytes.length > MAX_SYNC_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("OpenYSM sync payload is too large: " + safeBytes.length);
        }
        buf.writeInt(safeBytes.length);
        buf.writeBytes(safeBytes);
    }

    public static class Handler implements IMessageHandler<C2SModelSyncPayload17, IMessage> {

        @Override
        public IMessage onMessage(C2SModelSyncPayload17 message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (sender != null) {
                OpenYsmModelSyncServer.handlePayload(sender.getUniqueID(), message.data);
            }
            return null;
        }
    }
}
