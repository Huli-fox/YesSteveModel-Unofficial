package com.fox.ysmu.geckolib3.resource;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import com.fox.ysmu.geckolib3.file.AnimationFile;
import com.fox.ysmu.geckolib3.file.AnimationFileLoader;
import com.fox.ysmu.geckolib3.molang.MolangRegistrar;
import com.fox.ysmu.geckolib3.network.NetworkHandler;
import com.fox.ysmu.geckolib3.network.PacketRemoveAnimation;
import com.fox.ysmu.geckolib3.network.PacketSendAnimation;
import com.fox.ysmu.geckolib3.watchers.AnimationDirWatcher;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AnimationLibrary {
    private HashMap<ResourceLocation, AnimationFile> folderAnimations = new HashMap<>();
    public File folder;
    public AnimationDirWatcher updateController;
    public static AnimationLibrary instance;

    public AnimationLibrary(File folder) {
        instance = this;
        this.folder = folder;
        this.folder.mkdirs();
        updateController = new AnimationDirWatcher(folder);
        updateController.start();
    }

    public void reload(boolean sync) {
        folderAnimations.clear();
        recursiveWalk(this.folder);
        if (sync) syncAll();
    }

    public void syncPlayer(EntityPlayer player) {
        for (Map.Entry<ResourceLocation, AnimationFile> entry : folderAnimations.entrySet()) {
            NetworkHandler.sendToPlayer(new PacketSendAnimation(entry.getValue(), entry.getKey().getResourcePath()), player);
        }
    }

    public void syncAll() {
        if (MinecraftServer.getServer() != null) {
            for (Map.Entry<ResourceLocation, AnimationFile> entry : folderAnimations.entrySet()) {
                NetworkHandler.sendToAll(new PacketSendAnimation(entry.getValue(), entry.getKey().getResourcePath()));
            }
        }
    }

    public void syncAdd(ResourceLocation location) {
        if (MinecraftServer.getServer() != null) {
            NetworkHandler.sendToAll(new PacketSendAnimation(folderAnimations.get(location), location.getResourcePath()));
        }
    }

    public void syncRemove(ResourceLocation location) {
        if (MinecraftServer.getServer() != null) {
            NetworkHandler.sendToAll(new PacketRemoveAnimation(location.getResourcePath()));
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

    public AnimationFile get(ResourceLocation identifier) {
        return folderAnimations.get(identifier);
    }

    public void remove(File file) {
        ResourceLocation location = new ResourceLocation("custom", getNameFromFile(file));
        folderAnimations.remove(location);
        syncRemove(location);
    }

    public String getNameFromFile(File file) {
        String folderPath = folder.getAbsolutePath();
        return file.getAbsolutePath().substring(folderPath.length() + 1).replace('\\', '/');
    }

    public void storeModel(File file) {
        ResourceLocation location = new ResourceLocation("custom", getNameFromFile(file));
        AnimationFile model = loadModel(file);
        if (model != null) {
            folderAnimations.put(location, model);
            syncAdd(location);
        }
    }

    public AnimationFile loadModel(File file) {
        ResourceLocation location = new ResourceLocation("custom", getNameFromFile(file));
        try {
            return AnimationFileLoader.getInstance().loadAllAnimations(MolangRegistrar.getParser(), file, location);
        } catch (Exception e) {
            return null;
        }
    }
}
