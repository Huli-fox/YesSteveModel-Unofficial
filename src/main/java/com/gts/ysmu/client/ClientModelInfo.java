package com.gts.ysmu.client;

import com.gts.ysmu.geckolib3.file.ModelExtraResourcesFile;
import com.gts.ysmu.client.texture.OuterFileTexture;
import com.gts.ysmu.model.format.ServerModelInfo;
import com.gts.ysmu.client.model.MainModelData;
import com.gts.ysmu.geckolib3.file.VehicleModelFiles;
import com.gts.ysmu.geckolib3.file.ProjectileModelFiles;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class ClientModelInfo {

    private final MainModelData mainModelData;

    private final ProjectileModelFiles[] projectileModelFiles;

    private final VehicleModelFiles[] vehicleModelFiles;

    private final ModelExtraResourcesFile extraResources;

    @NotNull
    private final ServerModelInfo info;

    private final Map<String, OuterFileTexture> avatarTextures;

    private final Map<String, OuterFileTexture> guiTextures;

    public ClientModelInfo(MainModelData mainModelData, ProjectileModelFiles[] projectileModelFiles, VehicleModelFiles[] vehicleModelFiles, ModelExtraResourcesFile extraResources, @NotNull ServerModelInfo info, Map<String, OuterFileTexture> avatarTextures, Map<String, OuterFileTexture> guiTextures) {
        this.mainModelData = mainModelData;
        this.projectileModelFiles = projectileModelFiles;
        this.vehicleModelFiles = vehicleModelFiles;
        this.extraResources = extraResources;
        this.info = info;
        this.avatarTextures = avatarTextures;
        this.guiTextures = guiTextures;
    }

    public MainModelData getMainModelData() {
        return this.mainModelData;
    }

    public ProjectileModelFiles[] getExtraItemModels() {
        return this.projectileModelFiles;
    }

    public VehicleModelFiles[] getVehicleModelFiles() {
        return this.vehicleModelFiles;
    }

    public ModelExtraResourcesFile getExtraResources() {
        return this.extraResources;
    }

    public Map<String, OuterFileTexture> getAvatarTextures() {
        return this.avatarTextures;
    }

    public Map<String, OuterFileTexture> getGuiTextures() {
        return this.guiTextures;
    }

    @NotNull
    public ServerModelInfo getInfo() {
        return this.info;
    }
}
