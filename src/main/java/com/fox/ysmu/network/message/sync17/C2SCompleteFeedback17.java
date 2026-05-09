package com.fox.ysmu.network.message.sync17;

import net.minecraft.entity.player.EntityPlayerMP;

import com.fox.ysmu.network.sync.ModelSyncServer17;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2SCompleteFeedback17 implements IMessage {

    private boolean success;
    private String message;

    public C2SCompleteFeedback17() {}

    public C2SCompleteFeedback17(boolean success, String message) {
        this.success = success;
        this.message = message == null ? "" : message;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.success = buf.readBoolean();
        this.message = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.success);
        ByteBufUtils.writeUTF8String(buf, this.message);
    }

    public static class Handler implements IMessageHandler<C2SCompleteFeedback17, IMessage> {

        @Override
        public IMessage onMessage(C2SCompleteFeedback17 message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player != null) {
                ModelSyncServer17.handleCompleteFeedback(player, message.success, message.message);
            }
            return null;
        }
    }
}
