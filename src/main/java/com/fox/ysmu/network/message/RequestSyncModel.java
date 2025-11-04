package com.fox.ysmu.network.message;

import com.fox.ysmu.client.ClientModelManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class RequestSyncModel implements IMessage {

    public RequestSyncModel() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    @SideOnly(Side.CLIENT)
    public static class Handler implements IMessageHandler<RequestSyncModel, IMessage> {

        @Override
        public IMessage onMessage(RequestSyncModel message, MessageContext ctx) {
            ClientModelManager.sendSyncModelMessage();
            return null;
        }
    }
}
