package com.gts.ysmu.network.message;

import com.gts.ysmu.model.ServerModelManager;
import com.gts.ysmu.capability.ModelInfoCapabilityProvider;
import com.gts.ysmu.resource.models.ModelProperties;
import com.gts.ysmu.geckolib3.core.molang.util.StringPool;
import com.gts.ysmu.util.data.OrderedStringMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.function.Supplier;

public class C2SPlayAnimationPacket {

    private final int animationIndex;

    private final String category;

    public C2SPlayAnimationPacket(int animationIndex, String category) {
        this.animationIndex = animationIndex;
        this.category = category;
    }

    public static C2SPlayAnimationPacket createDefault() {
        return new C2SPlayAnimationPacket(-1, StringPool.EMPTY);
    }

    public static void encode(C2SPlayAnimationPacket message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.animationIndex);
        buf.writeUtf(message.category);
    }

    public static C2SPlayAnimationPacket decode(FriendlyByteBuf buf) {
        return new C2SPlayAnimationPacket(buf.readVarInt(), buf.readUtf());
    }

    public static void handle(C2SPlayAnimationPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null) {
                    return;
                }
                handleCapability(message, sender);
            });
        }
        context.setPacketHandled(true);
    }

    private static void handleCapability(C2SPlayAnimationPacket message, ServerPlayer sender) {
        sender.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(modelInfoCap -> {
            if (message.animationIndex == -1) {
                modelInfoCap.stopAnimation(sender);
            } else {
                ServerModelManager.getModelDefinition(modelInfoCap.getModelId()).ifPresent(serverModelCap -> {
                    OrderedStringMap<String, String> extraAnimations;
                    ModelProperties modelProperties = serverModelCap.getLoadedModelData().getModelProperties();
                    Map<String, OrderedStringMap<String, String>> extraAnimationClassify = modelProperties.getExtraAnimationClassify();
                    if (StringUtils.isNotBlank(message.category) && extraAnimationClassify.containsKey(message.category)) {
                        extraAnimations = extraAnimationClassify.get(message.category);
                    } else {
                        extraAnimations = modelProperties.getExtraAnimation();
                    }
                    if (extraAnimations.size() > message.animationIndex) {
                        modelInfoCap.playAnimation(sender, extraAnimations.getKeyAt(message.animationIndex));
                    }
                });
            }
        });
    }
}
