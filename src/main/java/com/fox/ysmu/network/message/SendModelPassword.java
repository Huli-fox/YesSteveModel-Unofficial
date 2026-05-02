package com.fox.ysmu.network.message;

import java.util.UUID;

import com.fox.ysmu.client.ClientModelManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public class SendModelPassword implements IMessage {

    private UUID playerId;
    private byte[] password;

    public SendModelPassword() {}

    public SendModelPassword(UUID playerId, byte[] password) {
        this.playerId = playerId;
        this.password = password;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.playerId = new UUID(buf.readLong(), buf.readLong());
        this.password = new byte[buf.readInt()];
        buf.readBytes(this.password);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.playerId.getMostSignificantBits());
        buf.writeLong(this.playerId.getLeastSignificantBits());
        buf.writeInt(this.password.length);
        buf.writeBytes(this.password);
    }

    public static class Handler implements IMessageHandler<SendModelPassword, IMessage> {

        @Override
        public IMessage onMessage(SendModelPassword message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                ClientModelManager.PASSWORD_UUID = message.playerId;
                ClientModelManager.PASSWORD = message.password;
            }
            return null;
        }
    }
}
