package com.gts.ysmu.network.message;

import com.gts.ysmu.model.ServerModelManager;
import com.gts.ysmu.capability.ModelInfoCapabilityProvider;
import com.gts.ysmu.config.ServerConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SRequestSwitchModelPacket {

    private final String modelId;

    private final String textureId;

    public C2SRequestSwitchModelPacket(String modelId, String textureId) {
        this.modelId = modelId;
        this.textureId = textureId;
    }

    public static void encode(C2SRequestSwitchModelPacket message, FriendlyByteBuf buf) {
        buf.writeUtf(message.modelId);
        buf.writeUtf(message.textureId);
    }

    public static C2SRequestSwitchModelPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestSwitchModelPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(C2SRequestSwitchModelPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender != null && ServerConfig.CAN_SWITCH_MODEL.get()) {
                    handleCapability(message, sender);
                }
            });
        }
        context.setPacketHandled(true);
    }

    private static void handleCapability(C2SRequestSwitchModelPacket message, ServerPlayer sender) {
        sender.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(cap -> {
            String str = message.modelId;
            if (!ServerModelManager.getServerModelInfo().containsKey(str) || !ServerModelManager.getServerModelInfo().get(str).getModelInfo().getTextures().contains(message.textureId)) {
                cap.resetToDefault();
            } else {
                cap.setModelAndTexture(message.modelId, message.textureId);
            }
            cap.stopAnimation(sender);
        });
    }
}
