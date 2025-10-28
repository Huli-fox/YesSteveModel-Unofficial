package com.fox.ysmu.event.api;

import com.fox.ysmu.client.entity.CustomPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class SpecialPlayerRenderEvent extends Event {
    private final Player player;
    private final CustomPlayerEntity customPlayer;
    private final ResourceLocation modelId;

    public SpecialPlayerRenderEvent(Player player, CustomPlayerEntity customPlayer, ResourceLocation modelId) {
        this.player = player;
        this.customPlayer = customPlayer;
        this.modelId = modelId;
    }

    public Player getPlayer() {
        return player;
    }

    public CustomPlayerEntity getCustomPlayer() {
        return customPlayer;
    }

    public ResourceLocation getModelId() {
        return modelId;
    }
}
