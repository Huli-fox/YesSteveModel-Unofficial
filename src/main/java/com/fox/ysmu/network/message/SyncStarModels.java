package com.fox.ysmu.network.message;

import com.fox.ysmu.eep.ExtendedStarModels;
import com.google.common.collect.Sets;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.util.Set;

public class SyncStarModels implements IMessage {
    private Set<ResourceLocation> starModels;

    public SyncStarModels() {
    }

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

    @SideOnly(Side.CLIENT)
    public static class Handler implements IMessageHandler<SyncStarModels, IMessage> {
        @Override
        public IMessage onMessage(SyncStarModels message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                handleEEP(message);
            }
            return null;
        }
        private void handleEEP(SyncStarModels message) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                ExtendedStarModels eep = ExtendedStarModels.get(mc.thePlayer);
                if (eep != null) {
                    eep.setStarModels(message.starModels);
                }
            }
        }
    }
}
