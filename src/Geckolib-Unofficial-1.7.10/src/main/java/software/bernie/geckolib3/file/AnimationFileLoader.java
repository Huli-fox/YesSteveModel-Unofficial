package software.bernie.geckolib3.file;

import com.eliotlash.molang.MolangParser;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import software.bernie.geckolib3.core.builder.Animation;
import software.bernie.geckolib3.util.json.JsonAnimationUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

public class AnimationFileLoader {
    private static AnimationFileLoader instance;

    public static AnimationFileLoader getInstance() {
        if (instance == null) {
            instance = new AnimationFileLoader();
        }
        return instance;
    }

    public AnimationFile loadAllAnimations(MolangParser parser, ResourceLocation location, IResourceManager manager) {
        AnimationFile animationFile = new AnimationFile();
        JsonObject jsonRepresentation = loadFile(location, manager);
        Set<Map.Entry<String, JsonElement>> entrySet = JsonAnimationUtils.getAnimations(jsonRepresentation);
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            String animationName = entry.getKey();
            Animation animation;
            try {
                animation = JsonAnimationUtils.deserializeJsonToAnimation(
                    JsonAnimationUtils.getAnimation(jsonRepresentation, animationName), parser);
                animationFile.putAnimation(animationName, animation);
            } catch (GeckoJsonException e) {
                System.err.println("[GeckoLib] Could not load animation: {}" + animationName + e);
                throw new RuntimeException(e);
            }
        }
        return animationFile;
    }

    public AnimationFile loadAllAnimations(MolangParser parser, File file, ResourceLocation location) {
        AnimationFile animationFile = new AnimationFile();
        JsonObject jsonRepresentation = loadFile(file, location);
        Set<Map.Entry<String, JsonElement>> entrySet = JsonAnimationUtils.getAnimations(jsonRepresentation);
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            String animationName = entry.getKey();
            Animation animation;
            try {
                animation = JsonAnimationUtils.deserializeJsonToAnimation(
                    JsonAnimationUtils.getAnimation(jsonRepresentation, animationName), parser);
                animationFile.putAnimation(animationName, animation);
            } catch (GeckoJsonException e) {
                System.err.println("[GeckoLib] Could not load animation: {}" + animationName + e);
                throw new RuntimeException(e);
            }
        }
        return animationFile;
    }

    /**
     * Internal method for handling reloads of animation files. Do not override.
     */
    private JsonObject loadFile(ResourceLocation location, IResourceManager manager) {
        String content = getResourceAsString(location, manager);
        Gson GSON = new Gson();
        return gsonDeserialize(GSON, new StringReader(content), JsonObject.class, false);
    }

    private JsonObject loadFile(File file, ResourceLocation location) {
        String content = getResourceAsString(file, location);
        Gson GSON = new Gson();
        return gsonDeserialize(GSON, new StringReader(content), JsonObject.class, false);
    }

    @Nullable
    public static <T> T gsonDeserialize(Gson gsonIn, Reader readerIn, Class<T> adapter, boolean lenient) {
        try {
            JsonReader jsonreader = new JsonReader(readerIn);
            jsonreader.setLenient(lenient);
            return (T) gsonIn.getAdapter(adapter).read(jsonreader);
        } catch (IOException ioexception) {
            throw new JsonParseException(ioexception);
        }
    }

    public static String getResourceAsString(ResourceLocation location, IResourceManager manager) {
        try (InputStream inputStream = manager.getResource(location).getInputStream()) {
            return IOUtils.toString(inputStream, Charset.defaultCharset());
        } catch (Exception e) {
            String message = "Couldn't load " + location;
            System.err.println("[GeckoLib] " + message + e);
            throw new RuntimeException(new FileNotFoundException(location.toString()));
        }
    }

    public static String getResourceAsString(File file, ResourceLocation location) {
        try (InputStream inputStream = file.toURL().openStream()) {
            return IOUtils.toString(inputStream, Charset.defaultCharset());
        } catch (Exception e) {
            String message = "Couldn't load " + location;
            System.err.println("[GeckoLib] " + message + e);
            throw new RuntimeException(new FileNotFoundException(location.toString()));
        }
    }
}
