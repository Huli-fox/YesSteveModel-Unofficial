package com.eliotlash.mclib.utils.resources;

import net.minecraft.util.ResourceLocation;
import software.bernie.example.config.ConfigHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Texture location
 * <p>
 * A hack class that allows to use uppercase characters in the path 1.11.2 and
 * up.
 */
public class TextureLocation extends ResourceLocation {
    public TextureLocation(String domain, String path) {
        super(domain, path);

        this.set(domain, path);
    }

    public TextureLocation(String string) {
        super(string);

        this.set(string);
    }

    public void set(String location) {
        String[] split = location.split(":");
        String domain = split.length > 0 ? split[0] : "minecraft";
        String path = split.length > 1 ? split[1] : "";

        this.set(domain, path);
    }

    public void set(String domain, String path) {
        /* Guess what it does */
        Field[] fields = ResourceLocation.class.getDeclaredFields();

        for (Field field : fields) {
            try {
                this.unlockField(field);
            } catch (Exception e) {
                if (ConfigHandler.debugPrintStacktraces) {
                    e.printStackTrace();
                }
            }
        }

        try {
            fields[0].set(this, domain);
            fields[1].set(this, path);
        } catch (Exception e) {
            if (ConfigHandler.debugPrintStacktraces) {
                e.printStackTrace();
            }
        }
    }

    protected void unlockField(Field field) throws Exception {
        field.setAccessible(true);

        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }
}
