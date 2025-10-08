package com.fox.ysmu;

import com.fox.ysmu.command.CommandTransform;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.registry.EntityRegistry;
import net.minecraft.client.renderer.entity.RenderManager;
import com.fox.ysmu.client.renderer.entity.RenderDisguiseGecko;
import com.fox.ysmu.entity.EntityDisguiseGecko;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.IResourcePack;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;


@Mod(modid = "ysmu", name = "ysmu", version = "0.1")
public class ysmu {

    public static final String MODID = "ysmu";
    private static int modEntityId = 0;
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide().isClient()) {
            injectExternalModelDirectory();
        }

        // 注册实体
        EntityRegistry.registerModEntity(EntityDisguiseGecko.class, "DisguiseGecko",
                modEntityId++, this, 64, 1, true);

        // 注册渲染器 (仅客户端)
        if (event.getSide().isClient()) {
            // 【修正】RenderDisguiseGecko的构造函数现在是无参的
            // 【修正】RenderManager的获取方式是 RenderManager.instance
            RenderingRegistry.registerEntityRenderingHandler(EntityDisguiseGecko.class, new RenderDisguiseGecko());
            // 我们需要将RenderManager传递给渲染器的manager字段，GeckoLib的内部机制需要它
            RenderDisguiseGecko renderer = new RenderDisguiseGecko();
            renderer.setRenderManager(RenderManager.instance);
            RenderingRegistry.registerEntityRenderingHandler(EntityDisguiseGecko.class, renderer);
        }

        TransformationEventHandler handler = new TransformationEventHandler();

        // 注册通用事件
        FMLCommonHandler.instance().bus().register(handler);

        // 注册Forge事件（包含客户端专有的Render事件）
        MinecraftForge.EVENT_BUS.register(handler);
    }
    private void injectExternalModelDirectory() {
        File modelDir = new File(FMLClientHandler.instance().getClient().mcDataDir, "model");
        if (!modelDir.exists() || !modelDir.isDirectory()) {
            return;
        }

        try {
            FolderResourcePack resourcePack = new FolderResourcePack(modelDir);

            // 1. 获取 FML 内部的资源包列表
            //    这个列表在cpw.mods.fml.client.FMLClientHandler中，字段名为 "resourcePackList"
            Field resourcePackListField = FMLClientHandler.class.getDeclaredField("resourcePackList");
            resourcePackListField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<IResourcePack> fmlResourcePacks = (List<IResourcePack>) resourcePackListField.get(FMLClientHandler.instance());

            // 2. 将我们的资源包添加进去
            fmlResourcePacks.add(resourcePack);

            System.out.println("[TransformMod] Successfully injected external model directory via FMLClientHandler!");

        } catch (Exception e) {
            System.err.println("[TransformMod] Failed to inject external model directory!");
            e.printStackTrace();
        }
    }
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandTransform());
    }
}