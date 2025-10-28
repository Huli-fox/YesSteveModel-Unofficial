//package com.fox.ysmu.bukkit.message;
//
//import com.fox.ysmu.client.gui.PlayerModelScreen;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.player.LocalPlayer;
//import net.minecraft.network.FriendlyByteBuf;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.player.Player;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.api.distmarker.OnlyIn;
//import net.minecraftforge.network.NetworkEvent;
//
//import java.util.function.Supplier;
//
//public class OpenModelGuiMessage {
//    public static int CURRENT_NPC_ID = -1;
//    private final int entityId;
//    private final int npcId;
//
//    public OpenModelGuiMessage(int entityId, int npcId) {
//        this.entityId = entityId;
//        this.npcId = npcId;
//    }
//
//    public static void encode(OpenModelGuiMessage message, FriendlyByteBuf buf) {
//    }
//
//    public static OpenModelGuiMessage decode(FriendlyByteBuf buf) {
//        return new OpenModelGuiMessage(buf.readInt(), buf.readInt());
//    }
//
//    public static void handle(OpenModelGuiMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
//        NetworkEvent.Context context = contextSupplier.get();
//        if (context.getDirection().getReceptionSide().isClient()) {
//            context.enqueueWork(() -> handleMessage(message));
//        }
//        context.setPacketHandled(true);
//    }
//
//    @OnlyIn(Dist.CLIENT)
//    private static void handleMessage(OpenModelGuiMessage message) {
//        LocalPlayer localPlayer = Minecraft.getInstance().player;
//        if (localPlayer != null) {
//            Entity entity = localPlayer.level().getEntity(message.entityId);
//            if (entity instanceof Player player) {
//                CURRENT_NPC_ID = message.npcId;
//                Minecraft.getInstance().setScreen(new PlayerModelScreen(player));
//            }
//        }
//    }
//}
