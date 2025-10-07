package software.bernie.geckolib3.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class PacketSendModel implements IMessage, IMessageHandler<PacketSendModel, IMessage> {
    private GeoModel model;
    private String resLoc;

    public PacketSendModel() {
    }

    public PacketSendModel(GeoModel model, String resLoc) {
        this.model = model;
        this.resLoc = resLoc;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            resLoc = ByteBufUtils.readUTF8String(buf);
            byte[] bytes = new byte[buf.readInt()];
            buf.readBytes(bytes);
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
            GeoModel model = (GeoModel) objectInputStream.readObject();
            objectInputStream.close();
            this.model = model;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(model);
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
    public IMessage onMessage(PacketSendModel message, MessageContext ctx) {
        if (message.model != null) {
            HashMap<ResourceLocation, GeoModel> models = GeckoLibCache.getInstance().getGeoModels();
            ResourceLocation resourceLocation = new ResourceLocation("custom", message.resLoc);
            if (models.containsKey(resourceLocation)) {
                models.remove(resourceLocation);
            }
            models.put(resourceLocation, message.model);
        }
        return null;
    }
}
