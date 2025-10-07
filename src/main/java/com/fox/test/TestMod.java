package com.fox.test;

import com.fox.test.client.renderer.entity.CustomEntityRenderer;
import com.fox.test.client.renderer.entity.PlayerGeoRenderer;
import com.fox.test.entity.CustomEntity;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;

@Mod(modid = "test", name = "Test Mod", version = "0.2", dependencies = "required-after:geckolib3")
public class TestMod {

    public static TestMod instance;
    private PlayerGeoRenderer playerGeoRenderer;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        instance = this;
        // 注册实体
        // "CustomEntity" 是注册名，0 是一个唯一的实体ID
        EntityRegistry.registerModEntity(CustomEntity.class, "CustomEntity", 0, instance, 64, 1, true);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // 注册渲染器，这一步必须在客户端执行
        if (event.getSide().isClient()) {
            RenderingRegistry.registerEntityRenderingHandler(CustomEntity.class, new CustomEntityRenderer());
            
            // 初始化Geckolib玩家渲染器
            playerGeoRenderer = new PlayerGeoRenderer();
            
            // 注册事件监听器
            MinecraftForge.EVENT_BUS.register(this);
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        // 取消原版玩家渲染
        event.setCanceled(true);
        
        // 使用Geckolib渲染器渲染玩家
        if (playerGeoRenderer != null) {
            playerGeoRenderer.renderPlayer(event.entityPlayer, 
                    event.entityPlayer.posX - RenderManager.renderPosX, 
                    event.entityPlayer.posY - RenderManager.renderPosY, 
                    event.entityPlayer.posZ - RenderManager.renderPosZ, 
                    event.entityPlayer.rotationYaw, 
                    event.partialRenderTick);
        }
    }
}