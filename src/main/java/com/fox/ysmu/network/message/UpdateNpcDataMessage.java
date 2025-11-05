package com.fox.ysmu.network.message;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import com.fox.ysmu.data.NPCData;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class UpdateNpcDataMessage implements IMessage {

    private UUID uuid;
    private ResourceLocation modelId;
    private ResourceLocation textureId;

    public UpdateNpcDataMessage() {}

    public UpdateNpcDataMessage(UUID uuid, ResourceLocation modelId, ResourceLocation textureId) {
        this.uuid = uuid;
        this.modelId = modelId;
        this.textureId = textureId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        long mostSig = buf.readLong();
        long leastSig = buf.readLong();
        this.uuid = new UUID(mostSig, leastSig);
        this.modelId = new ResourceLocation(ByteBufUtils.readUTF8String(buf));
        this.textureId = new ResourceLocation(ByteBufUtils.readUTF8String(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.uuid.getMostSignificantBits());
        buf.writeLong(this.uuid.getLeastSignificantBits());
        ByteBufUtils.writeUTF8String(buf, this.modelId.toString());
        ByteBufUtils.writeUTF8String(buf, this.textureId.toString());
    }

    public static class Handler implements IMessageHandler<UpdateNpcDataMessage, IMessage> {

        @Override
        public IMessage onMessage(UpdateNpcDataMessage message, MessageContext ctx) {
            if (Minecraft.getMinecraft().thePlayer != null) {
                NPCData.put(message.uuid, message.modelId, message.textureId);
            }
            return null;
        }
    }
}
