package com.fox.ysmu.network.message;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SetNpcModelAndTexture implements IMessage {

    private String modelId;
    private String selectTexture;
    private int npcId;

    public SetNpcModelAndTexture() {}

    public SetNpcModelAndTexture(net.minecraft.util.ResourceLocation modelId,
        net.minecraft.util.ResourceLocation selectTexture, int npcId) {
        this.modelId = modelId.toString();
        this.selectTexture = selectTexture.toString();
        this.npcId = npcId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.modelId = ByteBufUtils.readUTF8String(buf);
        this.selectTexture = ByteBufUtils.readUTF8String(buf);
        this.npcId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.modelId);
        ByteBufUtils.writeUTF8String(buf, this.selectTexture);
        buf.writeInt(this.npcId);
    }

    public static class Handler implements IMessageHandler<SetNpcModelAndTexture, IMessage> {

        @Override
        public IMessage onMessage(SetNpcModelAndTexture message, MessageContext ctx) {
            return null;
        }
    }
}
