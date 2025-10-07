package software.bernie.geckolib3.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.util.HashMap;

public class PacketRemoveModel implements IMessage, IMessageHandler<PacketRemoveModel, IMessage> {
    private String resLoc;

    public PacketRemoveModel() {
    }

    public PacketRemoveModel(String resLoc) {
        this.resLoc = resLoc;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        resLoc = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, resLoc);
    }

    @Override
    public IMessage onMessage(PacketRemoveModel message, MessageContext ctx) {
        HashMap<ResourceLocation, GeoModel> models = GeckoLibCache.getInstance().getGeoModels();
        ResourceLocation resourceLocation = new ResourceLocation("custom", message.resLoc);
        models.remove(resourceLocation);
        return null;
    }
}
