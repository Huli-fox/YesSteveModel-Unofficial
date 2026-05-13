package com.gts.ysmu.network.message;

import com.gts.ysmu.model.ServerModelManager;
import com.gts.ysmu.capability.ModelInfoCapabilityProvider;
import com.gts.ysmu.capability.StarModelsCapabilityProvider;
import com.gts.ysmu.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SVersionCheckPacket {

    private final String version;

    public C2SVersionCheckPacket() {
        this(NetworkHandler.VERSION);
    }

    public C2SVersionCheckPacket(String version) {
        this.version = version;
    }

    public static C2SVersionCheckPacket decode(FriendlyByteBuf buf) {
        return new C2SVersionCheckPacket(buf.readUtf());
    }

    public static void encode(C2SVersionCheckPacket message, FriendlyByteBuf buf) {
        buf.writeUtf(message.version);
    }

    public static void handle(C2SVersionCheckPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null && NetworkHandler.setChannelVersion(context.getNetworkManager(), message.version)) {
            ServerModelManager.validatePlayerModel(sender);
            sender.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(cap -> {
                cap.setMandatory(false);
                cap.stopAnimation(sender);
            });
            sender.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).ifPresent(cap -> {
                NetworkHandler.sendToClientPlayer(new S2CSyncStarModelsPacket(cap.getStarModels()), sender);
            });
            ServerModelManager.requestPlayerModels(sender, null);
        }
        context.setPacketHandled(true);
    }
}
