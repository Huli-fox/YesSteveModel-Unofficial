package com.fox.ysmu.event.api;

import com.fox.ysmu.client.entity.CustomPlayerEntity;
import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

@Cancelable
public class SpecialPlayerRenderEvent extends Event {
    private final EntityPlayer player;
    private final CustomPlayerEntity customPlayer;
    private final ResourceLocation modelId;

    public SpecialPlayerRenderEvent(EntityPlayer player, CustomPlayerEntity customPlayer, ResourceLocation modelId) {
        this.player = player;
        this.customPlayer = customPlayer;
        this.modelId = modelId;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public CustomPlayerEntity getCustomPlayer() {
        return customPlayer;
    }

    public ResourceLocation getModelId() {
        return modelId;
    }
}
