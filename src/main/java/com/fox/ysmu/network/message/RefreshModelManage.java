package com.fox.ysmu.network.message;

import com.fox.ysmu.command.sub.ManageCommand;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.network.NetworkHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

public class RefreshModelManage implements IMessage {
    public RefreshModelManage() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    @SideOnly(Side.SERVER)
    public static class Handler implements IMessageHandler<RefreshModelManage, IMessage> {
        @Override
        public IMessage onMessage(RefreshModelManage message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (sender != null && sender.canCommandSenderUseCommand(4, "")) {
                List<RequestServerModelInfo.Info> customInfo = ManageCommand.getFilesInfo(ServerModelManager.CUSTOM);
                List<RequestServerModelInfo.Info> authInfo = ManageCommand.getFilesInfo(ServerModelManager.AUTH);
                NetworkHandler.sendToClientPlayer(new RequestServerModelInfo(customInfo, authInfo), sender);
            }
            return null;
        }
    }
}
