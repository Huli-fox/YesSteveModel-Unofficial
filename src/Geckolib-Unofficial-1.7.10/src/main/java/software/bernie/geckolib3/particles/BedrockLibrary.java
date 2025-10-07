package software.bernie.geckolib3.particles;

import com.eliotlash.mclib.utils.JsonUtils;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import software.bernie.geckolib3.watchers.ParticleDirWatcher;
import software.bernie.example.config.ConfigHandler;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class BedrockLibrary {
    public static long lastUpdate;

    public Map<String, BedrockScheme> presets = new HashMap<String, BedrockScheme>();
    public Map<String, BedrockScheme> factory = new HashMap<String, BedrockScheme>();
    public File folder;
    public ParticleDirWatcher updateController;
    public static BedrockLibrary instance;

    public BedrockLibrary(File folder) {
        instance = this;
        this.folder = folder;
        this.folder.mkdirs();
        updateController = new ParticleDirWatcher(folder);
        updateController.start();
    }

    public File file(String name) {
        return new File(this.folder, name + ".json");
    }

    public boolean hasEffect(String name) {
        return this.file(name).isFile();
    }

    public void reload() {
        this.presets.clear();
        this.presets.putAll(this.factory);
        recursiveWalk(this.folder);
    }

    public void recursiveWalk(File folder) {
        for (File file : folder.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                this.storeScheme(file);
            }
            if (file.isDirectory()) {
                recursiveWalk(file);
            }
        }
    }

    public BedrockScheme get(String identifier) {
        for (BedrockScheme scheme : presets.values()) {
            if (scheme.identifier.equals(identifier)) {
                return scheme;
            }
        }
        return null;
    }

    public void remove(String name) {

        name = name.substring(0, name.indexOf(".json"));
        if (presets.containsKey(name)) {
            presets.remove(name);
        }
    }

    public void storeScheme(File file) {
        BedrockScheme scheme = this.loadScheme(file);

        if (scheme != null) {
            String name = file.getName();
            String schemeName = name.substring(0, name.indexOf(".json"));
            if (presets.containsKey(schemeName)) {
                presets.get(schemeName).toReload = true;
            }
            scheme.name = schemeName;
            this.presets.put(schemeName, scheme);
        }
    }

    /**
     * Load a scheme from a file
     */
    public BedrockScheme loadScheme(File file) {
        if (!file.exists()) {
            return null;
        }

        try {
            String contents = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            if (contents.isEmpty()) {
                return null;
            }
            return BedrockScheme.parse(contents);
        } catch (Exception e) {
            if (ConfigHandler.debugPrintStacktraces) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public void storeFactory(ResourceLocation name) {
        BedrockScheme scheme = this.loadFactory(name);

        if (scheme != null) {
            scheme.name = getName(name);
            this.factory.put(getName(name), scheme);
        }
    }

    /**
     * Load a scheme from Blockbuster's zip
     */
    public BedrockScheme loadFactory(ResourceLocation resLoc) {
        try {
            return BedrockScheme.parse(IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("assets/" + resLoc.getResourceDomain() + "/" + resLoc.getResourcePath()), StandardCharsets.UTF_8)).factory(true);
        } catch (Exception e) {
            if (ConfigHandler.debugPrintStacktraces) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public String getName(ResourceLocation resLoc) {
        String[] parts = resLoc.getResourcePath().split("/");
        String name = parts[parts.length - 1];
        return name.substring(0, name.indexOf(".json"));
    }

    public void save(String filename, BedrockScheme scheme) {
        String json = JsonUtils.jsonToPretty(BedrockScheme.toJson(scheme));
        File file = this.file(filename);

        try {
            FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
        }

        this.storeScheme(file);

        lastUpdate = System.currentTimeMillis();
    }
}
