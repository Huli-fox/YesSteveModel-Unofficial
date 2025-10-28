package com.fox.ysmu.network.message;

import com.fox.ysmu.eep.ExtendedModelInfo;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

public class SetPlayAnimation implements IMessage {
    private static final int STOP = -1;
    private int extraAnimationId;

    public SetPlayAnimation() {
    }

    public SetPlayAnimation(int extraAnimationId) {
        this.extraAnimationId = extraAnimationId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.extraAnimationId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.extraAnimationId);
    }

    @SideOnly(Side.SERVER)
    public static class Handler implements IMessageHandler<SetPlayAnimation, IMessage> {
        @Override
        public IMessage onMessage(SetPlayAnimation message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (sender != null && STOP <= message.extraAnimationId && message.extraAnimationId < 8) {
                handleEEP(message, sender);
            }
            return null;
        }
        private void handleEEP(SetPlayAnimation message, EntityPlayerMP sender) {
            ExtendedModelInfo modelIdEEP = ExtendedModelInfo.get(sender);
            if (modelIdEEP != null) {
                if (message.extraAnimationId == STOP) {
                    modelIdEEP.stopAnimation();
                } else {
                    modelIdEEP.playAnimation("extra" + message.extraAnimationId);
                }
            }
        }
    }
}
