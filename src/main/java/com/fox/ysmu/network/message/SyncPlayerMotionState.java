package com.fox.ysmu.network.message;

import java.util.UUID;

import com.fox.ysmu.ysmu;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public class SyncPlayerMotionState implements IMessage {

    private UUID playerId;
    private byte flags;

    public SyncPlayerMotionState() {}

    public SyncPlayerMotionState(UUID playerId, byte flags) {
        this.playerId = playerId;
        this.flags = flags;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.playerId = new UUID(buf.readLong(), buf.readLong());
        this.flags = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.playerId.getMostSignificantBits());
        buf.writeLong(this.playerId.getLeastSignificantBits());
        buf.writeByte(this.flags);
    }

    public static class Handler implements IMessageHandler<SyncPlayerMotionState, IMessage> {

        @Override
        public IMessage onMessage(SyncPlayerMotionState message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                ysmu.proxy.handlePlayerMotionState(message);
            }
            return null;
        }
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public byte getFlags() {
        return flags;
    }
}
