package com.fox.ysmu.network.message;

import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.network.NetworkHandler;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;

public class UploadFile implements IMessage {
    private String name;
    private byte[] fileBytes;
    private int dirOrdinal;

    public UploadFile() {
    }

    public UploadFile(String name, byte[] fileBytes, Dir dir) {
        this.name = name;
        this.fileBytes = fileBytes;
        this.dirOrdinal = dir.ordinal();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.name = ByteBufUtils.readUTF8String(buf);
        this.fileBytes = new byte[buf.readInt()];
        buf.readBytes(this.fileBytes);
        this.dirOrdinal = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.name);
        buf.writeInt(this.fileBytes.length);
        buf.writeBytes(this.fileBytes);
        buf.writeInt(this.dirOrdinal);
    }

    @SideOnly(Side.SERVER)
    public static class Handler implements IMessageHandler<UploadFile, IMessage> {
        @Override
        public IMessage onMessage(UploadFile message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (sender != null && sender.canCommandSenderUseCommand(4, "")) {
                writeFile(message, sender);
            }
            return null;
        }
        private void writeFile(UploadFile message, EntityPlayerMP player) {
            Path filePath;
            Dir dir = Dir.values()[message.dirOrdinal];
            if (dir == Dir.CUSTOM) {
                filePath = ServerModelManager.CUSTOM.resolve(message.name);
            } else {
                filePath = ServerModelManager.AUTH.resolve(message.name);
            }
            try {
                FileUtils.writeByteArrayToFile(filePath.toFile(), message.fileBytes);
                NetworkHandler.sendToClientPlayer(new CompleteFeedback(), player);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public enum Dir {
        CUSTOM,
        AUTH
    }
}
