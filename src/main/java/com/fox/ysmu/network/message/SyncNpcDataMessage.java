package com.fox.ysmu.network.message;

import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import com.fox.ysmu.data.NPCData;
import com.google.common.collect.Maps;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.Pair;

public class SyncNpcDataMessage implements IMessage {

    private Map<UUID, Pair<ResourceLocation, ResourceLocation>> data;

    public SyncNpcDataMessage() {}

    public SyncNpcDataMessage(Map<UUID, Pair<ResourceLocation, ResourceLocation>> data) {
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.data = Maps.newHashMap();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            long mostSig = buf.readLong();
            long leastSig = buf.readLong();
            UUID uuid = new UUID(mostSig, leastSig);
            ResourceLocation modelId = new ResourceLocation(ByteBufUtils.readUTF8String(buf));
            ResourceLocation textureId = new ResourceLocation(ByteBufUtils.readUTF8String(buf));
            this.data.put(uuid, Pair.of(modelId, textureId));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.data.size());
        for (Map.Entry<UUID, Pair<ResourceLocation, ResourceLocation>> entry : this.data.entrySet()) {
            UUID uuid = entry.getKey();
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
            Pair<ResourceLocation, ResourceLocation> pair = entry.getValue();
            ByteBufUtils.writeUTF8String(
                buf,
                pair.first()
                    .toString());
            ByteBufUtils.writeUTF8String(
                buf,
                pair.second()
                    .toString());
        }
    }

    public static class Handler implements IMessageHandler<SyncNpcDataMessage, IMessage> {

        @Override
        public IMessage onMessage(SyncNpcDataMessage message, MessageContext ctx) {
            if (Minecraft.getMinecraft().thePlayer != null) {
                NPCData.addAll(message.data);
            }
            return null;
        }
    }
}
