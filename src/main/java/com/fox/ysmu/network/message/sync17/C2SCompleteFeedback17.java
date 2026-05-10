package com.fox.ysmu.network.message;

import net.minecraft.entity.player.EntityPlayerMP;

import com.fox.ysmu.network.sync.OpenYsmModelSyncServer;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2SCompleteFeedback17 implements IMessage {

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_FAILED = 1;

    private int status;
    private int loaded;
    private int downloaded;
    private int cacheHits;
    private String message = "";

    public C2SCompleteFeedback17() {}

    public C2SCompleteFeedback17(int status, int loaded, int downloaded, int cacheHits, String message) {
        this.status = status;
        this.loaded = loaded;
        this.downloaded = downloaded;
        this.cacheHits = cacheHits;
        this.message = message == null ? "" : message;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.status = buf.readInt();
        this.loaded = buf.readInt();
        this.downloaded = buf.readInt();
        this.cacheHits = buf.readInt();
        this.message = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.status);
        buf.writeInt(this.loaded);
        buf.writeInt(this.downloaded);
        buf.writeInt(this.cacheHits);
        ByteBufUtils.writeUTF8String(buf, this.message);
    }

    public int getStatus() {
        return status;
    }

    public int getLoaded() {
        return loaded;
    }

    public int getDownloaded() {
        return downloaded;
    }

    public int getCacheHits() {
        return cacheHits;
    }

    public String getMessage() {
        return message;
    }

    public static class Handler implements IMessageHandler<C2SCompleteFeedback17, IMessage> {

        @Override
        public IMessage onMessage(C2SCompleteFeedback17 message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (sender != null) {
                OpenYsmModelSyncServer.complete(
                    sender.getUniqueID(),
                    message.status,
                    message.loaded,
                    message.downloaded,
                    message.cacheHits,
                    message.message);
            }
            return null;
        }
    }
}
