package com.fox.ysmu.network.message;

import static com.fox.ysmu.model.ServerModelManager.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayerMP;

import org.apache.commons.io.FileUtils;

import com.fox.ysmu.data.EncryptTools;
import com.fox.ysmu.model.format.ServerModelInfo;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.util.ThreadTools;
import com.fox.ysmu.util.UuidUtils;
import com.google.common.collect.Lists;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SyncModelFiles implements IMessage {

    private String[] md5Info;

    public SyncModelFiles() {}

    public SyncModelFiles(String[] md5Info) {
        this.md5Info = md5Info;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        this.md5Info = new String[count];
        for (int i = 0; i < count; i++) {
            this.md5Info[i] = ByteBufUtils.readUTF8String(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.md5Info.length);
        for (String md5 : this.md5Info) {
            ByteBufUtils.writeUTF8String(buf, md5);
        }
    }

    public static class Handler implements IMessageHandler<SyncModelFiles, IMessage> {

        @Override
        public IMessage onMessage(SyncModelFiles message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (sender != null) {
                sendPassword(sender);
                sendModelFiles(message.md5Info, sender);
            }
            return null;
        }

        private void sendModelFiles(String[] md5Info, EntityPlayerMP sender) {
            Collection<String> cache = CACHE_NAME_INFO.values()
                .stream()
                .map(ServerModelInfo::getMd5)
                .collect(Collectors.toList());
            List<String> output = Lists.newArrayList(cache);
            for (String md5 : md5Info) {
                if (cache.contains(md5)) {
                    output.remove(md5);
                    NetworkHandler.sendToClientPlayer(new RequestLoadModel(md5), sender);
                }
            }
            for (String md5 : output) {
                File modelFile = CACHE_SERVER.resolve(md5)
                    .toFile();
                try {
                    byte[] modelBytes = FileUtils.readFileToByteArray(modelFile);
                    ThreadTools.THREAD_POOL
                        .submit(() -> NetworkHandler.sendToClientPlayer(new SendModelFile(modelBytes), sender));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendPassword(EntityPlayerMP sender) {
            try {
                byte[] password = FileUtils.readFileToByteArray(PASSWORD_FILE.toFile());
                byte[] uuid = UuidUtils.asBytes(sender.getUniqueID());
                byte[] output = EncryptTools.encryptPassword(uuid, password);
                ThreadTools.THREAD_POOL
                    .submit(() -> NetworkHandler.sendToClientPlayer(new SendModelFile(output), sender));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
