package com.fox.ysmu.network.message;

import com.fox.ysmu.client.upload.UploadManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class CompleteFeedback implements IMessage {

    public CompleteFeedback() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    @SideOnly(Side.CLIENT)
    public static class Handler implements IMessageHandler<CompleteFeedback, IMessage> {

        @Override
        public IMessage onMessage(CompleteFeedback message, MessageContext ctx) {
            UploadManager.finishUpload();
            return null;
        }
    }
}
