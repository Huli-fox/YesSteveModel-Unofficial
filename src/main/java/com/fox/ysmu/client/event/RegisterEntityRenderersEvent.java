package com.fox.ysmu.client.event;

import com.fox.ysmu.ysmu;
import com.fox.ysmu.client.renderer.CustomPlayerRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = ysmu.MODID)
public class RegisterEntityRenderersEvent {
    private static CustomPlayerRenderer CUSTOM_PLAYER_RENDERER;

    @SubscribeEvent
    public static void clientSetup(EntityRenderersEvent.RegisterRenderers event) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        ItemInHandRenderer itemInHandRenderer = dispatcher.getItemInHandRenderer();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        EntityModelSet entityModels = Minecraft.getInstance().getEntityModels();
        Font font = Minecraft.getInstance().font;
        EntityRendererProvider.Context context = new EntityRendererProvider.Context(dispatcher, itemRenderer, blockRenderer, itemInHandRenderer, resourceManager, entityModels, font);
        context.getModelSet().onResourceManagerReload(resourceManager);
        CUSTOM_PLAYER_RENDERER = new CustomPlayerRenderer(context);
    }

    public static CustomPlayerRenderer getInstance() {
        return CUSTOM_PLAYER_RENDERER;
    }
}
