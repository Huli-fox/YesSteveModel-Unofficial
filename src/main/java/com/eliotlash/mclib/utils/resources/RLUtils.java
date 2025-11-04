package com.eliotlash.mclib.utils.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.SimpleResource;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * {@link ResourceLocation} utility methods
 * <p>
 * This class has utils for saving and reading {@link ResourceLocation} from
 * actor model and skin.
 */
public class RLUtils {

    private static List<IResourceTransformer> transformers = new ArrayList<IResourceTransformer>();
    private static ResourceLocation pixel = new ResourceLocation("mclib:textures/pixel.png");

    /**
     * Get stream for multi resource location
     */
    @SideOnly(Side.CLIENT)
    public static IResource getStreamForMultiskin(MultiResourceLocation multi) throws IOException {
        if (multi.children.isEmpty()) {
            throw new IOException("Multi-skin is empty!");
        }

        try {
            // if (true)//TODO
            // {
            // MultiskinThread.add(multi);
            //
            // return Minecraft.getMinecraft().getResourceManager().getResource(pixel);
            // }
            // else
            // {
            // MultiskinThread.clear();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(TextureProcessor.postProcess(multi), "png", stream);

            return new SimpleResource(multi, new ByteArrayInputStream(stream.toByteArray()), null, null);
            // }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static void register(IResourceTransformer transformer) {
        transformers.add(transformer);
    }

    public static ResourceLocation create(String path) {
        for (IResourceTransformer transformer : transformers) {
            path = transformer.transform(path);
        }

        return new TextureLocation(path);
    }

    public static ResourceLocation create(String domain, String path) {
        for (IResourceTransformer transformer : transformers) {
            String newDomain = transformer.transformDomain(domain, path);
            String newPath = transformer.transformPath(domain, path);

            domain = newDomain;
            path = newPath;
        }

        return new TextureLocation(domain, path);
    }

    public static ResourceLocation create(NBTBase base) {
        ResourceLocation location = MultiResourceLocation.from(base);

        if (location != null) {
            return location;
        }

        if (base instanceof NBTTagString) {
            return create(((NBTTagString) base).func_150285_a_());
        }

        return null;
    }

    public static ResourceLocation create(JsonElement element) {
        ResourceLocation location = MultiResourceLocation.from(element);

        if (location != null) {
            return location;
        }

        if (element.isJsonPrimitive()) {
            return create(element.getAsString());
        }

        return null;
    }

    public static NBTBase writeNbt(ResourceLocation location) {
        if (location instanceof IWritableLocation) {
            return ((IWritableLocation) location).writeNbt();
        } else if (location != null) {
            return new NBTTagString(location.toString());
        }

        return null;
    }

    public static JsonElement writeJson(ResourceLocation location) {
        if (location instanceof IWritableLocation) {
            return ((IWritableLocation) location).writeJson();
        } else if (location != null) {
            return new JsonPrimitive(location.toString());
        }

        return JsonNull.INSTANCE;
    }

    public static ResourceLocation clone(ResourceLocation location) {
        if (location instanceof IWritableLocation) {
            Object copy = ((IWritableLocation) location).copy();

            if (copy instanceof ResourceLocation) {
                return (ResourceLocation) copy;
            }
        }

        if (location != null) {
            return create(location.toString());
        }

        return null;
    }
}
