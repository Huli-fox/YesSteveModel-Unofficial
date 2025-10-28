package com.fox.ysmu.geckolib3.resource;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import com.fox.ysmu.geckolib3.file.GeoModelLoader;
import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.fox.ysmu.geckolib3.network.NetworkHandler;
import com.fox.ysmu.geckolib3.network.PacketRemoveModel;
import com.fox.ysmu.geckolib3.network.PacketSendModel;
import com.fox.ysmu.geckolib3.watchers.ModelDirWatcher;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ModelLibrary {
    private HashMap<ResourceLocation, GeoModel> folderModels = new HashMap<>();
    public File folder;
    public ModelDirWatcher updateController;
    public static ModelLibrary instance;

    public ModelLibrary(File folder) {
        instance = this;
        this.folder = folder;
        this.folder.mkdirs();
        updateController = new ModelDirWatcher(folder);
        updateController.start();
    }

    public void reload(boolean sync) {
        folderModels.clear();
        recursiveWalk(this.folder);
        if (sync) syncAll();
    }

    public void syncPlayer(EntityPlayer player) {
        for (Map.Entry<ResourceLocation, GeoModel> entry : folderModels.entrySet()) {
            NetworkHandler.sendToPlayer(new PacketSendModel(entry.getValue(), entry.getKey().getResourcePath()), player);
        }
    }

    public void syncAll() {
        if (MinecraftServer.getServer() != null) {
            for (Map.Entry<ResourceLocation, GeoModel> entry : folderModels.entrySet()) {
                NetworkHandler.sendToAll(new PacketSendModel(entry.getValue(), entry.getKey().getResourcePath()));
            }
        }
    }

    public void syncAdd(ResourceLocation location) {
        if (MinecraftServer.getServer() != null) {
            NetworkHandler.sendToAll(new PacketSendModel(folderModels.get(location), location.getResourcePath()));
        }
    }

    public void syncRemove(ResourceLocation location) {
        if (MinecraftServer.getServer() != null) {
            NetworkHandler.sendToAll(new PacketRemoveModel(location.getResourcePath()));
        }
    }

    public void recursiveWalk(File folder) {
        for (File file : folder.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                this.storeModel(file);
            }
            if (file.isDirectory()) {
                recursiveWalk(file);
            }
        }
    }

    public GeoModel get(ResourceLocation identifier) {
        return folderModels.get(identifier);
    }

    public void remove(File file) {
        ResourceLocation location = new ResourceLocation("custom", getNameFromFile(file));
        folderModels.remove(location);
        syncRemove(location);
    }

    public String getNameFromFile(File file) {
        String folderPath = folder.getAbsolutePath();
        return file.getAbsolutePath().substring(folderPath.length() + 1).replace('\\', '/');
    }

    public void storeModel(File file) {
        ResourceLocation location = new ResourceLocation("custom", getNameFromFile(file));
        GeoModel model = loadModel(file);
        if (model != null) {
            folderModels.put(location, model);
            syncAdd(location);
        }
    }

    public GeoModel loadModel(File file) {
        ResourceLocation location = new ResourceLocation("custom", getNameFromFile(file));
        try {
            return GeoModelLoader.getInstance().loadModelFromFile(file, location);
        } catch (Exception e) {
            return null;
        }
    }
}
