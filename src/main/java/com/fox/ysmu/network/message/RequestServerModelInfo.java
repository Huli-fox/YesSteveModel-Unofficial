package com.fox.ysmu.network.message;


import java.util.List;
//import com.fox.ysmu.client.gui.ModelManageScreen;
import com.fox.ysmu.model.format.Type;
import com.google.common.collect.Lists;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;

public class RequestServerModelInfo implements IMessage {

    private List<Info> customModels;
    private List<Info> authModels;

    public RequestServerModelInfo() {}

    public RequestServerModelInfo(List<Info> customModels, List<Info> authModels) {
        this.customModels = customModels;
        this.authModels = authModels;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int customModelsSize = buf.readInt();
        this.customModels = Lists.newArrayList();
        for (int i = 0; i < customModelsSize; i++) {
            this.customModels.add(bufferToInfo(buf));
        }

        int authModelsSize = buf.readInt();
        this.authModels = Lists.newArrayList();
        for (int i = 0; i < authModelsSize; i++) {
            this.authModels.add(bufferToInfo(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.customModels.size());
        for (Info info : this.customModels) {
            infoToBuffer(buf, info);
        }
        buf.writeInt(this.authModels.size());
        for (Info info : this.authModels) {
            infoToBuffer(buf, info);
        }
    }

    public static class Handler implements IMessageHandler<RequestServerModelInfo, IMessage> {

        @Override
        public IMessage onMessage(RequestServerModelInfo message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                //Minecraft.getMinecraft().displayGuiScreen(new ModelManageScreen(message.customModels, message.authModels));
            }
            return null;
        }
    }

    private static void infoToBuffer(ByteBuf buf, Info info) {
        ByteBufUtils.writeUTF8String(buf, info.fileName);
        // 在1.7.10中没有直接的枚举写入方法，我们需要手动处理
        buf.writeInt(info.type.ordinal());
        buf.writeLong(info.size);
    }

    private static Info bufferToInfo(ByteBuf buf) {
        String fileName = ByteBufUtils.readUTF8String(buf);
        // 在1.7.10中没有直接的枚举读取方法，我们需要手动处理
        Type type = Type.values()[buf.readInt()];
        long size = buf.readLong();
        return new Info(fileName, type, size);
    }

    public static class Info {

        private String fileName;
        private Type type;
        private long size;

        public Info() {}

        public Info(String fileName, Type type, long size) {
            this.fileName = fileName;
            this.type = type;
            this.size = size;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }
}
