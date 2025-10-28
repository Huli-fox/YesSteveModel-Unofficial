package com.fox.ysmu.network.message;

import com.fox.ysmu.model.ServerModelManager;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class HandleFile implements IMessage {
    private String name;
    private int dirOrdinal;
    private String action;
    private String rename;

    public HandleFile() {
    }

    public HandleFile(String name, Dir dir, String action, String rename) {
        this.name = name;
        this.dirOrdinal = dir.ordinal();
        this.action = action;
        this.rename = rename;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.name = ByteBufUtils.readUTF8String(buf);
        this.dirOrdinal = buf.readInt();
        this.action = ByteBufUtils.readUTF8String(buf);
        this.rename = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.name);
        buf.writeInt(this.dirOrdinal);
        ByteBufUtils.writeUTF8String(buf, this.action);
        ByteBufUtils.writeUTF8String(buf, this.rename);
    }

    @SideOnly(Side.SERVER)
    public static class Handler implements IMessageHandler<HandleFile, IMessage> {
        @Override
        public IMessage onMessage(HandleFile message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            // TODO 混淆名注意
            if (sender != null && MinecraftServer.getServer().getConfigurationManager().func_152596_g(sender.getGameProfile())) {
                handleFileOperation(message);
            }
            return null;
        }

        private void handleFileOperation(HandleFile message) {
            Dir dirType = Dir.values()[message.dirOrdinal];

            if (dirType == Dir.CUSTOM) {
                String actionIn = message.action;
                File file = ServerModelManager.CUSTOM.resolve(message.name).toFile();
                if (file.isFile() || file.isDirectory()) {
                    if (actionIn.equals("delete")) {
                        FileUtils.deleteQuietly(file);
                    }
                    if (actionIn.equals("move")) {
                        File destFile = ServerModelManager.AUTH.resolve(message.name).toFile();
                        try {
                            if (file.isFile()) {
                                FileUtils.moveFile(file, destFile);
                            }
                            if (file.isDirectory()) {
                                FileUtils.moveDirectory(file, destFile);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (actionIn.equals("rename") && message.rename != null && !message.rename.isEmpty()) {
                        File destFile = ServerModelManager.CUSTOM.resolve(message.rename).toFile();
                        try {
                            if (file.isFile()) {
                                FileUtils.moveFile(file, destFile);
                            }
                            if (file.isDirectory()) {
                                FileUtils.moveDirectory(file, destFile);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (dirType == Dir.AUTH) {
                String actionIn = message.action;
                File file = ServerModelManager.AUTH.resolve(message.name).toFile();
                if (file.isFile() || file.isDirectory()) {
                    if (actionIn.equals("delete")) {
                        FileUtils.deleteQuietly(file);
                    }
                    if (actionIn.equals("move")) {
                        File destFile = ServerModelManager.CUSTOM.resolve(message.name).toFile();
                        try {
                            if (file.isFile()) {
                                FileUtils.moveFile(file, destFile);
                            }
                            if (file.isDirectory()) {
                                FileUtils.moveDirectory(file, destFile);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (actionIn.equals("rename") && message.rename != null && !message.rename.isEmpty()) {
                        File destFile = ServerModelManager.AUTH.resolve(message.rename).toFile();
                        try {
                            if (file.isFile()) {
                                FileUtils.moveFile(file, destFile);
                            }
                            if (file.isDirectory()) {
                                FileUtils.moveDirectory(file, destFile);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public enum Dir {
        CUSTOM,
        AUTH
    }
}
