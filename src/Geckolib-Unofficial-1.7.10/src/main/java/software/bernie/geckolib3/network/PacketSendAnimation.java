package software.bernie.geckolib3.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.file.AnimationFile;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class PacketSendAnimation implements IMessage, IMessageHandler<PacketSendAnimation, IMessage> {
    private AnimationFile animationFile;
    private String resLoc;

    public PacketSendAnimation() {
    }

    public PacketSendAnimation(AnimationFile animationFile, String resLoc) {
        this.animationFile = animationFile;
        this.resLoc = resLoc;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            resLoc = ByteBufUtils.readUTF8String(buf);
            byte[] bytes = new byte[buf.readInt()];
            buf.readBytes(bytes);
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
            AnimationFile file = (AnimationFile) objectInputStream.readObject();
            objectInputStream.close();
            this.animationFile = file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(animationFile);
            objectOutputStream.flush();
            objectOutputStream.close();
            byte[] bytes = byteArrayOutputStream.toByteArray();
            ByteBufUtils.writeUTF8String(buf, resLoc);
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IMessage onMessage(PacketSendAnimation message, MessageContext ctx) {
        if (message.animationFile != null) {
            HashMap<ResourceLocation, AnimationFile> animations = GeckoLibCache.getInstance().getAnimations();
            ResourceLocation resourceLocation = new ResourceLocation("custom", message.resLoc);
            if (animations.containsKey(resourceLocation)) {
                animations.remove(resourceLocation);
            }
            animations.put(resourceLocation, message.animationFile);
        }
        return null;
    }
}
