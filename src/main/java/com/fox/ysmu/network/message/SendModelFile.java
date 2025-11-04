package com.fox.ysmu.network.message;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.commons.io.FileUtils;

import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.util.Md5Utils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class SendModelFile implements IMessage {

    private byte[] data;

    public SendModelFile() {}

    public SendModelFile(byte[] data) {
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.data = new byte[buf.readInt()];
        buf.readBytes(this.data);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.data.length);
        buf.writeBytes(this.data);
    }

    @SideOnly(Side.CLIENT)
    public static class Handler implements IMessageHandler<SendModelFile, IMessage> {

        @Override
        public IMessage onMessage(SendModelFile message, MessageContext ctx) {
            if (message.data.length == 48) {
                ClientModelManager.PASSWORD = message.data;
            } else {
                String fileName = Md5Utils.md5Hex(message.data)
                    .toUpperCase(Locale.US);
                File file = ServerModelManager.CACHE_CLIENT.resolve(fileName)
                    .toFile();
                try {
                    FileUtils.writeByteArrayToFile(file, message.data);
                    RequestLoadModel.loadModel(fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}
