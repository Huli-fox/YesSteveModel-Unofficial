package software.bernie.geckolib3.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.file.AnimationFile;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.util.HashMap;

public class PacketRemoveAnimation implements IMessage, IMessageHandler<PacketRemoveAnimation, IMessage> {
    private String resLoc;

    public PacketRemoveAnimation() {
    }

    public PacketRemoveAnimation(String resLoc) {
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
    public IMessage onMessage(PacketRemoveAnimation message, MessageContext ctx) {
        HashMap<ResourceLocation, AnimationFile> models = GeckoLibCache.getInstance().getAnimations();
        ResourceLocation resourceLocation = new ResourceLocation("custom", message.resLoc);
        models.remove(resourceLocation);
        return null;
    }
}
