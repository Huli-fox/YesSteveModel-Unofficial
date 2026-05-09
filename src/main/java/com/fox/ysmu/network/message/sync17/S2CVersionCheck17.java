package com.fox.ysmu.network.message.sync17;

import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.sync.ModelSyncClient17;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public class S2CVersionCheck17 implements IMessage {

    public static final int PROTOCOL_VERSION = 1;

    private int protocolVersion = PROTOCOL_VERSION;

    public S2CVersionCheck17() {}

    public S2CVersionCheck17(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.protocolVersion = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.protocolVersion);
    }

    public static class Handler implements IMessageHandler<S2CVersionCheck17, IMessage> {

        @Override
        public IMessage onMessage(S2CVersionCheck17 message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                boolean supported = message.protocolVersion == PROTOCOL_VERSION;
                if (supported) {
                    ModelSyncClient17.startVersionedSync();
                }
                NetworkHandler.CHANNEL.sendToServer(new C2SVersionCheck17(PROTOCOL_VERSION, supported));
            }
            return null;
        }
    }
}
