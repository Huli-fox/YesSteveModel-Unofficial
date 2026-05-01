package com.fox.ysmu.network.message;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import com.fox.ysmu.eep.ExtendedAuthModels;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.ysmu;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SetModelAndTexture implements IMessage {

    private String modelId;
    private String selectTexture;

    public SetModelAndTexture() {}

    public SetModelAndTexture(ResourceLocation modelId, ResourceLocation selectTexture) {
        this.modelId = modelId.toString();
        this.selectTexture = selectTexture.toString();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.modelId = ByteBufUtils.readUTF8String(buf);
        this.selectTexture = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.modelId);
        ByteBufUtils.writeUTF8String(buf, this.selectTexture);
    }

    public static class Handler implements IMessageHandler<SetModelAndTexture, IMessage> {

        @Override
        public IMessage onMessage(SetModelAndTexture message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (sender != null) {
                handleEEP(message, sender);
            }
            return null;
        }

        private void handleEEP(SetModelAndTexture message, EntityPlayerMP sender) {
            ExtendedModelInfo modelInfo = ExtendedModelInfo.get(sender);
            ExtendedAuthModels ownModelsEEP = ExtendedAuthModels.get(sender);
            if (modelInfo != null && ownModelsEEP != null) {
                ResourceLocation modelLoc = message.modelId.isEmpty() ? null : new ResourceLocation(message.modelId);
                ResourceLocation textureLoc = message.selectTexture.isEmpty() ? null
                    : new ResourceLocation(message.selectTexture);

                if (canUseModel(modelLoc, ownModelsEEP)) {
                    modelInfo.setModelAndTexture(modelLoc, textureLoc);
                } else {
                    resetToDefaultModel(modelInfo);
                }
            }
        }

        private boolean canUseModel(ResourceLocation modelLoc, ExtendedAuthModels ownModelsEEP) {
            return modelLoc == null || !ServerModelManager.AUTH_MODELS.contains(modelLoc.getResourcePath())
                || ownModelsEEP.containModel(modelLoc);
        }

        private void resetToDefaultModel(ExtendedModelInfo modelInfo) {
            ResourceLocation defaultModelId = new ResourceLocation(ysmu.MODID, "default");
            ResourceLocation defaultTextureId = new ResourceLocation(ysmu.MODID, "default/default.png");
            modelInfo.setModelAndTexture(defaultModelId, defaultTextureId);
        }
    }
}
