package com.elfmcys.yesstevemodel;

import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.ModSoundEvents;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.util.obfuscate.Keep;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Locale;

/**
 * TODO:
 * 默认模型应该就在模组架加载的时候就预加载了
 * 其它模型统统都是进入世界后加载
 */
@Mod(YesSteveModel.MOD_ID)
public class YesSteveModel {

    public static final String MOD_ID = "yes_steve_model";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public YesSteveModel() {
        initConfig();
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static void initConfig() {
        File oldConfig = FMLPaths.CONFIGDIR.get().resolve("yes_steve_model-common.toml").toFile();
        if (oldConfig.isFile()) {
            File file2 = FMLPaths.CONFIGDIR.get().resolve("yes_steve_model-client.toml").toFile();
            if (!file2.isFile()) {
                oldConfig.renameTo(file2);
            } else {
                oldConfig.delete();
            }
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, GeneralConfig.buildSpec());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.buildSpec());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModSoundEvents.REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    @Keep
    public static boolean isAvailable() {
        return true;
    }

    public static boolean isOnAndroid() {
        String runtimeName = System.getProperty("java.runtime.name", "").toLowerCase(Locale.ROOT);
        return System.getenv("MOD_ANDROID_RUNTIME") != null
                || System.getenv("FCL_VERSION_CODE") != null
                || System.getenv("ZALITH_VERSION_CODE") != null
                || runtimeName.contains("android");
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendUnavailableMessage() {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            localPlayer.sendSystemMessage(getUnavailableComponent());
        }
    }

    public static Component getUnavailableComponent() {
        return Component.literal("Yes Steve Model is not initialized.");
    }

    public static String getErrorMessage() {
        return "Yes Steve Model is not initialized.";
    }
}
