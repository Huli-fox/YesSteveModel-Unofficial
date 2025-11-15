package com.fox.ysmu;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class Config {
    private static Configuration configuration;

    // General Config
    public static boolean DISCLAIMER_SHOW = true;
    public static boolean PRINT_ANIMATION_ROULETTE_MSG = true;
    public static boolean DISABLE_SELF_MODEL = false;
    public static boolean DISABLE_OTHER_MODEL = false;
    public static boolean DISABLE_SELF_HANDS = false;
    public static String DEFAULT_MODEL_ID = "default";
    public static String DEFAULT_MODEL_TEXTURE = "default.png";

    // Extra Player Screen Config
    public static boolean DISABLE_PLAYER_RENDER = false;
    public static int PLAYER_POS_X = 10;
    public static int PLAYER_POS_Y = 10;
    public static double PLAYER_SCALE = 40.0;
    public static double PLAYER_YAW_OFFSET = 5.0;

    /**
     * 在 Mod preInit 阶段调用，用于初始化配置文件并进行首次加载。
     * @param configFile a suggested configuration file from the FMLPreInitializationEvent.
     */
    public static void init(File configFile) {
        if (configuration == null) {
            configuration = new Configuration(configFile);
            sync(true); // true 表示执行加载操作
        }
    }

    /**
     * 在需要保存配置时（如关闭GUI）调用。
     */
    public static void save() {
        sync(false); // false 表示执行保存操作
    }

    /**
     * 根据参数决定加载或保存。
     * @param load 如果为 true，则从配置文件加载到静态变量；如果为 false，则从静态变量保存到配置文件。
     */
    private static void sync(boolean load) {
        if (load) {
            configuration.load();
        }

        // General config values
        DISCLAIMER_SHOW = syncBoolean("DisclaimerShow", Configuration.CATEGORY_GENERAL, DISCLAIMER_SHOW, "Whether to display disclaimer GUI", load);
        PRINT_ANIMATION_ROULETTE_MSG = syncBoolean("PrintAnimationRouletteMsg", Configuration.CATEGORY_GENERAL, PRINT_ANIMATION_ROULETTE_MSG, "Whether to print animation roulette play message", load);
        DISABLE_SELF_MODEL = syncBoolean("DisableSelfModel", Configuration.CATEGORY_GENERAL, DISABLE_SELF_MODEL, "Prevents rendering of self player's model", load);
        DISABLE_OTHER_MODEL = syncBoolean("DisableOtherModel", Configuration.CATEGORY_GENERAL, DISABLE_OTHER_MODEL, "Prevents rendering of other player's model", load);
        DISABLE_SELF_HANDS = syncBoolean("DisableSelfHands", Configuration.CATEGORY_GENERAL, DISABLE_SELF_HANDS, "Prevents rendering of self player's hand", load);
        DEFAULT_MODEL_ID = syncString("DefaultModelId", Configuration.CATEGORY_GENERAL, DEFAULT_MODEL_ID, "The default model ID when a player first enters the game", load);
        DEFAULT_MODEL_TEXTURE = syncString("DefaultModelTexture", Configuration.CATEGORY_GENERAL, DEFAULT_MODEL_TEXTURE, "The default model texture when a player first enters the game", load);

        // Extra player render config values
        DISABLE_PLAYER_RENDER = syncBoolean("DisablePlayerRender", "extra_player_render", DISABLE_PLAYER_RENDER, "Whether to display player", load);
        PLAYER_POS_X = syncInt("PlayerPosX", "extra_player_render", PLAYER_POS_X, "Player position x in screen", 0, Integer.MAX_VALUE, load);
        PLAYER_POS_Y = syncInt("PlayerPosY", "extra_player_render", PLAYER_POS_Y, "Player position y in screen", 0, Integer.MAX_VALUE, load);
        PLAYER_SCALE = syncDouble("PlayerScale", "extra_player_render", PLAYER_SCALE, "Player scale in screen", 8.0, 360.0, load);
        PLAYER_YAW_OFFSET = syncDouble("PlayerYawOffset", "extra_player_render", PLAYER_YAW_OFFSET, "Player yaw offset in screen", load);

        // 检查配置是否已更改，如果已更改，则保存
        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    private static boolean syncBoolean(String name, String category, boolean currentValue, String comment, boolean load) {
        Property prop = configuration.get(category, name, currentValue, comment);
        if (load) {
            return prop.getBoolean(currentValue);
        } else {
            prop.set(currentValue);
            return currentValue;
        }
    }

    private static String syncString(String name, String category, String currentValue, String comment, boolean load) {
        Property prop = configuration.get(category, name, currentValue, comment);
        if (load) {
            return prop.getString();
        } else {
            prop.set(currentValue);
            return currentValue;
        }
    }

    private static int syncInt(String name, String category, int currentValue, String comment, int min, int max, boolean load) {
        Property prop = configuration.get(category, name, currentValue, comment, min, max);
        if (load) {
            return prop.getInt(currentValue);
        } else {
            prop.set(currentValue);
            return currentValue;
        }
    }

    private static double syncDouble(String name, String category, double currentValue, String comment, double min, double max, boolean load) {
        Property prop = configuration.get(category, name, currentValue, comment, min, max);
        if (load) {
            return prop.getDouble(currentValue);
        } else {
            prop.set(currentValue);
            return currentValue;
        }
    }

    private static double syncDouble(String name, String category, double currentValue, String comment, boolean load) {
        Property prop = configuration.get(category, name, currentValue, comment);
        if (load) {
            return prop.getDouble(currentValue);
        } else {
            prop.set(currentValue);
            return currentValue;
        }
    }
}
