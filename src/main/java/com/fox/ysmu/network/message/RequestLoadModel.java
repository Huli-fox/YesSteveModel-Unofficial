package com.fox.ysmu.network.message;

import java.io.File;
import java.util.UUID;

import net.minecraft.client.Minecraft;

import org.apache.commons.io.FileUtils;

import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.data.EncryptTools;
import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.util.ThreadTools;
import com.fox.ysmu.util.UuidUtils;
import com.fox.ysmu.ysmu;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class RequestLoadModel implements IMessage {

    private static final int PASSWORD_WAIT_RETRIES = 40;
    private static final long PASSWORD_WAIT_MILLIS = 500L;

    private String fileName;

    public RequestLoadModel() {}

    public RequestLoadModel(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.fileName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.fileName);
    }

    public static class Handler implements IMessageHandler<RequestLoadModel, IMessage> {

        @Override
        public IMessage onMessage(RequestLoadModel message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                ClientModelManager.rememberCachedModel(message.fileName);
                loadModel(message.fileName);
            }
            return null;
        }
    }

    @SideOnly(Side.CLIENT)
    public static void loadModel(String fileName) {
        ThreadTools.THREAD_POOL.submit(() -> {
            try {
                // Decryption waits for SendModelPassword; resource registration returns to the client thread.
                int retries = 0;
                while ((ClientModelManager.PASSWORD == null || ClientModelManager.PASSWORD_UUID == null)
                    && retries < PASSWORD_WAIT_RETRIES) {
                    Thread.sleep(PASSWORD_WAIT_MILLIS);
                    retries++;
                }
                byte[] password = ClientModelManager.PASSWORD;
                UUID passwordUuid = ClientModelManager.PASSWORD_UUID;
                if (password == null || passwordUuid == null) {
                    ysmu.LOG.warn("Timed out waiting for YSM model password before loading cache file {}", fileName);
                    return;
                }
                if (Minecraft.getMinecraft().thePlayer != null) {
                    File modelFile = ServerModelManager.CACHE_CLIENT.resolve(fileName)
                        .toFile();
                    byte[] fileBytes = FileUtils.readFileToByteArray(modelFile);
                    ModelData data = EncryptTools
                        .decryptModel(UuidUtils.asBytes(passwordUuid), password, fileBytes);
                    if (data != null) {
                        Minecraft.getMinecraft()
                            .func_152344_a(() -> ClientModelManager.registerAll(data));
                    } else {
                        ysmu.LOG.warn("Failed to decrypt YSM model cache file {}", fileName);
                    }
                }
            } catch (Exception e) {
                ysmu.LOG.warn("Failed to load YSM model cache file " + fileName, e);
            }
        });
    }
}
