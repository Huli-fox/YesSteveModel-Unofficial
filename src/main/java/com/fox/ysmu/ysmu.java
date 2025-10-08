package com.fox.ysmu;

import com.fox.ysmu.command.CommandTransform;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.registry.EntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager; // 新增导入
import com.fox.ysmu.client.renderer.entity.RenderDisguiseGecko;
import com.fox.ysmu.entity.EntityDisguiseGecko;
import cpw.mods.fml.client.registry.RenderingRegistry;
import java.io.File;
import java.lang.reflect.Field; // 新增导入
import java.util.List; // 新增导入


@Mod(modid = "ysmu", name = "ysmu", version = "0.1")
public class ysmu {

    public static final String MODID = "ysmu";
    private static int modEntityId = 0;
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide().isClient()) {
            File modelDir = new File(Minecraft.getMinecraft().mcDataDir, "model");
            if (modelDir.exists() && modelDir.isDirectory()) {
                try {
                    // 使用反射来访问私有的 defaultResourcePacks 列表
                    Field field = Minecraft.class.getDeclaredField("defaultResourcePacks");
                    field.setAccessible(true);
                    List defaultPacks = (List) field.get(Minecraft.getMinecraft());
                    defaultPacks.add(modelDir);
                    System.out.println("[ysmu] Successfully loaded external model directory!");
                } catch (Exception e) {
                    System.err.println("[ysmu] Failed to load external model directory via reflection!");
                    e.printStackTrace();
                }
            }
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

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandTransform());
    }
}