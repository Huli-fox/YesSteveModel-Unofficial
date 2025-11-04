package com.fox.ysmu.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.util.ThreadTools;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class SyncModelInfo implements IMessage {

    private int entityId;
    private NBTTagCompound modelInfoNBT;

    public SyncModelInfo() {}

    public SyncModelInfo(int entityId, ExtendedModelInfo modelInfo) {
        this.entityId = entityId;
        this.modelInfoNBT = new NBTTagCompound();
        if (modelInfo != null) {
            modelInfo.saveNBTData(this.modelInfoNBT);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.modelInfoNBT = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        ByteBufUtils.writeTag(buf, this.modelInfoNBT);
    }

    @SideOnly(Side.CLIENT)
    public static class Handler implements IMessageHandler<SyncModelInfo, IMessage> {

        @Override
        public IMessage onMessage(SyncModelInfo message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                handleEEP(message);
            }
            return null;
        }

        private void handleEEP(SyncModelInfo message) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld != null) {
                ThreadTools.THREAD_POOL.submit(() -> {
                    try {
                        int time = 0;
                        while (mc.theWorld.getEntityByID(message.entityId) == null && time < 5) {
                            Thread.sleep(500);
                            time++;
                        }
                        net.minecraft.entity.Entity entity = mc.theWorld.getEntityByID(message.entityId);
                        if (entity instanceof EntityPlayer player) {
                            ExtendedModelInfo eep = ExtendedModelInfo.get(player);
                            if (eep != null) {
                                eep.loadNBTData(message.modelInfoNBT);
                            }
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }
}
