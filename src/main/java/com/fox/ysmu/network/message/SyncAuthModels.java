package com.fox.ysmu.network.message;

import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.fox.ysmu.ysmu;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public class SyncAuthModels implements IMessage {

    private Set<ResourceLocation> authModels;

    public SyncAuthModels() {}

    public SyncAuthModels(Set<ResourceLocation> authModels) {
        this.authModels = authModels;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        this.authModels = Sets.newHashSet();
        for (int i = 0; i < size; i++) {
            String resourceLocationStr = readString(buf);
            this.authModels.add(new ResourceLocation(resourceLocationStr));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.authModels.size());
        for (ResourceLocation modelId : this.authModels) {
            writeString(buf, modelId.toString());
        }
    }

    public static class Handler implements IMessageHandler<SyncAuthModels, IMessage> {

        @Override
        public IMessage onMessage(SyncAuthModels message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                ysmu.proxy.handleAuthModels(message);
            }
            return null;
        }
    }

    public Set<ResourceLocation> getAuthModels() {
        return authModels;
    }

    private static String readString(ByteBuf buf) {
        int length = buf.readInt();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = buf.readChar();
        }
        return new String(chars);
    }

    private static void writeString(ByteBuf buf, String str) {
        buf.writeInt(str.length());
        for (char c : str.toCharArray()) {
            buf.writeChar(c);
        }
    }
}
