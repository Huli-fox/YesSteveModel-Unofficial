package com.elfmcys.yesstevemodel.client.gui.metadata;

import com.elfmcys.yesstevemodel.client.texture.OuterFileTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ModelDisplayAssets {
    private final String selectedTexture;

    private final Map<String, OuterFileTexture> authorAvatars;

    private final Map<String, AbstractTexture> guiTextures;

    public ModelDisplayAssets(String selectedTexture, Map<String, OuterFileTexture> authorAvatars, Map<String, AbstractTexture> guiTextures) {
        this.selectedTexture = selectedTexture;
        this.authorAvatars = authorAvatars;
        this.guiTextures = guiTextures;
    }

    public String getSelectedTexture() {
        return this.selectedTexture;
    }

    public Map<String, OuterFileTexture> getAuthorAvatars() {
        return this.authorAvatars;
    }

    @Nullable
    public AbstractTexture getGuiForeground() {
        return this.guiTextures.get("gui_foreground");
    }

    @Nullable
    public AbstractTexture getGuiBackground() {
        return this.guiTextures.get("gui_background");
    }
}
