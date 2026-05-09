package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.AnimationRegister;
import com.elfmcys.yesstevemodel.client.input.ExtraAnimationKey;
import com.elfmcys.yesstevemodel.client.input.*;
import com.elfmcys.yesstevemodel.client.renderer.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

@Mod.EventBusSubscriber(value = {Dist.CLIENT}, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetupEvent {
    public static Object validateClientCapabilities() {
        try {
            int maxTexSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
            if (maxTexSize <= 0) {
                return Component.literal("YSM: OpenGL context not available");
            }
            try {
                int testShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
                if (testShader != 0) {
                    GL20.glDeleteShader(testShader);
                }
            } catch (Exception e) {
                return Component.literal("YSM: GL20 (shaders) not available");
            }

            // 预載入default模型，延遲至第一次渲染tick
            // 不能在FMLClientSetupEvent中同步執行ModelAssembler，會導致StackOverflow
            //ClientModelManager.schedulePreloadDefaultModel();
            return null; // 成功
        } catch (Exception e) {
            return Component.literal("YSM Client Init Failed: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        AnimationRegister.registerAnimationState();
        event.enqueueWork(ClientSetupEvent::checkClientInitialization);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PlayerModelToggleKey.KEY_MAPPING);
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        event.register(AnimationRouletteKey.KEY_ROULETTE);
        event.register(AnimationRouletteKey.KEY_LOCK);
        event.register(DebugAnimationKey.KEY_MAPPING);
        event.register(ExtraPlayerRenderKey.KEY_MAPPING);
        ExtraAnimationKey.registerKeyMappings(event);
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        event.registerAbove(VanillaGuiOverlay.DEBUG_TEXT.id(), "ysm_debug_info", AnimationDebugOverlay.createOverlay());
        event.registerAbove(VanillaGuiOverlay.DEBUG_TEXT.id(), "ysm_extra_player", new LoadingStateOverlay());
        event.registerAbove(VanillaGuiOverlay.DEBUG_TEXT.id(), "ysm_loading_state", new ModelSyncStateOverlay());
    }

    private static void checkClientInitialization() {
        Component component = (Component) validateClientCapabilities();
        if (component != null) {
            throw new RuntimeException("YSM Client Initialization Failed: " + component.getString(256));
        }
    }

}
