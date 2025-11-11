package com.fox.ysmu.network.message;

import com.fox.ysmu.client.gui.PlayerModelScreen;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class OpenModelGuiMessage implements IMessage {
    public static int CURRENT_NPC_ID = -1;
    private int entityId;
    private int npcId;

    public OpenModelGuiMessage() {}

    public OpenModelGuiMessage(int entityId, int npcId) {
        this.entityId = entityId;
        this.npcId = npcId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.npcId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeInt(this.npcId);
    }

    public static class Handler implements IMessageHandler<OpenModelGuiMessage, IMessage> {
        @Override
        public IMessage onMessage(OpenModelGuiMessage message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                handleMessage(message);
            }
            return null;
        }
    }

    private static void handleMessage(OpenModelGuiMessage message) {
        EntityPlayer localPlayer = Minecraft.getMinecraft().thePlayer;
        if (localPlayer != null) {
            Entity entity = localPlayer.worldObj.getEntityByID(message.entityId);
            if (entity instanceof EntityPlayer player) {
                CURRENT_NPC_ID = message.npcId;
                Minecraft.getMinecraft().displayGuiScreen(new PlayerModelScreen(player));
            }
        }
    }
}
