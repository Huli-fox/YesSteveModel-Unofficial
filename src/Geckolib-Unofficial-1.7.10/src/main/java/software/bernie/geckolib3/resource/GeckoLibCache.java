package software.bernie.geckolib3.resource;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.FMLFolderResourcePack;
import net.geckominecraft.client.resources.LegacyV2Adapter;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.GeckoLib;
import software.bernie.geckolib3.file.AnimationFile;
import software.bernie.geckolib3.file.AnimationFileLoader;
import software.bernie.geckolib3.file.GeoModelLoader;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.molang.MolangRegistrar;
import software.bernie.geckolib3.particles.BedrockLibrary;
import software.bernie.example.config.ConfigHandler;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("deprecation")
public class GeckoLibCache implements IResourceManagerReloadListener {
    private static GeckoLibCache INSTANCE;


    public HashMap<ResourceLocation, AnimationFile> getAnimations() {
        if (!GeckoLib.hasInitialized) {
            throw new RuntimeException("GeckoLib was never initialized! Please read the documentation!");
        }
        return animations;
    }

    public HashMap<ResourceLocation, GeoModel> getGeoModels() {
        if (!GeckoLib.hasInitialized) {
            throw new RuntimeException("GeckoLib was never initialized! Please read the documentation!");
        }
        return geoModels;
    }

    private HashMap<ResourceLocation, AnimationFile> animations = new HashMap<>();
    private HashMap<ResourceLocation, GeoModel> geoModels = new HashMap<>();


    protected GeckoLibCache() {

    }

    public static GeckoLibCache getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GeckoLibCache();
            return INSTANCE;
        }
        return INSTANCE;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        HashMap<ResourceLocation, AnimationFile> tempAnimations = new HashMap<>();
        HashMap<ResourceLocation, GeoModel> tempModels = new HashMap<>();
        List<IResourcePack> packs = this.getPacks();

        if (packs == null) {
            return;
        }

        for (IResourcePack pack : packs) {
            for (ResourceLocation location : this.getLocations(pack, "animations",
                fileName -> fileName.endsWith(".json"))) {
                try {
                    tempAnimations.put(location, AnimationFileLoader.getInstance().loadAllAnimations(MolangRegistrar.getParser(), location, resourceManager));
                } catch (Exception e) {
                    if (ConfigHandler.debugPrintStacktraces) {
                        e.printStackTrace();
                    }
                    System.err.println("[GeckoLib] " + "Error loading animation file \"" + location + "\"!" + e);
                }
            }

            for (ResourceLocation location : this.getLocations(pack, "geo", fileName -> fileName.endsWith(".json"))) {
                try {
                    tempModels.put(location, GeoModelLoader.getInstance().loadModel(resourceManager, location));
                } catch (Exception e) {
                    if (ConfigHandler.debugPrintStacktraces) {
                        e.printStackTrace();
                    }
                    System.err.println("[GeckoLib] " + "Error loading model file \"" + location + "\"!" + e);
                }
            }

            for (ResourceLocation location : this.getLocations(pack, "particles", fileName -> fileName.endsWith(".json"))) {
                try {
                    BedrockLibrary.instance.storeFactory(location);
                } catch (Exception e) {
                    if (ConfigHandler.debugPrintStacktraces) {
                        e.printStackTrace();
                    }
                    System.err.println("[GeckoLib] " + "Error loading model file \"" + location + "\"!" + e);
                }
            }
        }
        animations = tempAnimations;
        geoModels = tempModels;
        BedrockLibrary.instance.reload();
    }

    @SuppressWarnings("unchecked")
    private List<IResourcePack> getPacks() {
        try {
            Field field = FMLClientHandler.class.getDeclaredField("resourcePackList");
            field.setAccessible(true);

            return (List<IResourcePack>) field.get(FMLClientHandler.instance());
        } catch (Exception e) {
            System.err.println("[GeckoLib] " + "Error accessing resource pack list!" + e);
        }

        return null;
    }

    private List<ResourceLocation> getLocations(IResourcePack pack, String folder, Predicate<String> predicate) {
        if (pack instanceof net.geckominecraft.client.resources.LegacyV2Adapter) {
            net.geckominecraft.client.resources.LegacyV2Adapter adapter = (LegacyV2Adapter) pack;
            Field packField = null;

            for (Field field : adapter.getClass().getDeclaredFields()) {
                if (field.getType() == IResourcePack.class) {
                    packField = field;

                    break;
                }
            }

            if (packField != null) {
                packField.setAccessible(true);

                try {
                    return this.getLocations((IResourcePack) packField.get(adapter), folder, predicate);
                } catch (Exception e) {
                }
            }
        }

        List<ResourceLocation> locations = new ArrayList<ResourceLocation>();

        if (pack instanceof FolderResourcePack) {
            this.handleFolderResourcePack((FolderResourcePack) pack, folder, predicate, locations);
        } else if (pack instanceof FileResourcePack) {
            this.handleZipResourcePack((FileResourcePack) pack, folder, predicate, locations);
        }

        return locations;
    }

    /* Folder handling */

    private void handleFolderResourcePack(FolderResourcePack folderPack, String folder, Predicate<String> predicate,
                                          List<ResourceLocation> locations) {
        Field fileField = null;

        for (Field field : AbstractResourcePack.class.getDeclaredFields()) {
            if (field.getType() == File.class) {
                fileField = field;

                break;
            }
        }

        if (fileField != null) {
            fileField.setAccessible(true);

            try {
                File file = (File) fileField.get(folderPack);
                Set<String> domains = folderPack.getResourceDomains();

                if (folderPack instanceof FMLFolderResourcePack) {
                    domains.add(((FMLFolderResourcePack) folderPack).getFMLContainer().getModId());
                }

                for (String domain : domains) {
                    String prefix = "assets/" + domain + "/" + folder;
                    File pathFile = new File(file, prefix);

                    this.enumerateFiles(folderPack, pathFile, predicate, locations, domain, folder);
                }
            } catch (IllegalAccessException e) {
                System.err.println(e.toString());
            }
        }
    }

    private void enumerateFiles(FolderResourcePack folderPack, File parent, Predicate<String> predicate,
                                List<ResourceLocation> locations, String domain, String prefix) {
        File[] files = parent.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile() && predicate.test(file.getName())) {
                locations.add(new ResourceLocation(domain, prefix + "/" + file.getName()));
            } else if (file.isDirectory()) {
                this.enumerateFiles(folderPack, file, predicate, locations, domain, prefix + "/" + file.getName());
            }
        }
    }

    /* Zip handling */

    private void handleZipResourcePack(FileResourcePack filePack, String folder, Predicate<String> predicate,
                                       List<ResourceLocation> locations) {
        Field zipField = null;

        for (Field field : FileResourcePack.class.getDeclaredFields()) {
            if (field.getType() == ZipFile.class) {
                zipField = field;
                break;
            }
        }

        if (zipField == null) return;

        zipField.setAccessible(true);

        try {
            ZipFile zip = (ZipFile) zipField.get(filePack);
            if (zip == null) {
                System.out.println("[GeckoLib] handleZipResourcePack: zipField is null for {}" + filePack);
                return;
            }

            String zipFileName = new File(zip.getName()).getName();

            if ("CarpentersBlocksCachedResources.zip".equalsIgnoreCase(zipFileName)) {
                System.out.println("[GeckoLib] Skipping Carpenter's Blocks cached zip: {}" + zipFileName);
                return;
            }

            this.enumerateZipFile(filePack, folder, zip, predicate, locations);
        } catch (IllegalAccessException e) {
            System.err.println("[GeckoLib] Error accessing zip file: {}" + e.toString() + e);
        }
    }

    private void enumerateZipFile(FileResourcePack filePack, String folder, ZipFile file, Predicate<String> predicate,
                                  List<ResourceLocation> locations) {
        Set<String> domains = filePack.getResourceDomains();
        Enumeration<? extends ZipEntry> it = file.entries();

        while (it.hasMoreElements()) {
            String name = it.nextElement().getName();

            for (String domain : domains) {
                String assets = "assets/" + domain + "/";
                String path = assets + folder + "/";

                if (name.startsWith(path) && predicate.test(name)) {
                    locations.add(new ResourceLocation(domain, name.substring(assets.length())));
                }
            }
        }
    }
}
