package com.gts.ysmu.model;

import com.gts.ysmu.model.format.ServerModelData;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ModelLoadResult {

    private final boolean success;

    @Nullable
    private final Component errorMessage;

    private final Map<String, ServerModelData> modelDefinitions;

    public ModelLoadResult(boolean success, @Nullable Object errorMessage, Map<String, ServerModelData> map) {
        this.success = success;
        this.errorMessage = (Component) errorMessage;
        this.modelDefinitions = map == null ? Object2ReferenceMaps.emptyMap() : ImmutableMap.copyOf(map);
    }

    public boolean isSuccess() {
        return this.success;
    }

    @Nullable
    public Component getErrorMessage() {
        return this.errorMessage;
    }

    public Map<String, ServerModelData> getModelDefinitions() {
        return this.modelDefinitions;
    }

}
