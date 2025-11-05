package com.fox.ysmu.network.message;

import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.fox.ysmu.ysmu;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public class SyncStarModels implements IMessage {

    private Set<ResourceLocation> starModels;

    public SyncStarModels() {}

    public SyncStarModels(Set<ResourceLocation> starModels) {
        this.starModels = starModels;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        this.starModels = Sets.newHashSet();
        for (int i = 0; i < size; i++) {
            String modelIdStr = ByteBufUtils.readUTF8String(buf);
            this.starModels.add(new ResourceLocation(modelIdStr));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.starModels.size());
        for (ResourceLocation modelId : this.starModels) {
            ByteBufUtils.writeUTF8String(buf, modelId.toString());
        }
    }

    public static class Handler implements IMessageHandler<SyncStarModels, IMessage> {

        @Override
        public IMessage onMessage(SyncStarModels message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                ysmu.proxy.handleStarModels(message);
            }
            return null;
        }
    }

    public Set<ResourceLocation> getStarModels() {
        return starModels;
    }
}
