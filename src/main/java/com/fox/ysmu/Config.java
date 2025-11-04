package com.fox.ysmu;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

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

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);
        configuration.load();

        // General config values
        DISCLAIMER_SHOW = configuration
            .get(Configuration.CATEGORY_GENERAL, "DisclaimerShow", DISCLAIMER_SHOW, "Whether to display disclaimer GUI")
            .getBoolean(DISCLAIMER_SHOW);

        PRINT_ANIMATION_ROULETTE_MSG = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "PrintAnimationRouletteMsg",
                PRINT_ANIMATION_ROULETTE_MSG,
                "Whether to print animation roulette play message")
            .getBoolean(PRINT_ANIMATION_ROULETTE_MSG);

        DISABLE_SELF_MODEL = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "DisableSelfModel",
                DISABLE_SELF_MODEL,
                "Prevents rendering of self player's model")
            .getBoolean(DISABLE_SELF_MODEL);

        DISABLE_OTHER_MODEL = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "DisableOtherModel",
                DISABLE_OTHER_MODEL,
                "Prevents rendering of other player's model")
            .getBoolean(DISABLE_OTHER_MODEL);

        DISABLE_SELF_HANDS = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "DisableSelfHands",
                DISABLE_SELF_HANDS,
                "Prevents rendering of self player's hand")
            .getBoolean(DISABLE_SELF_HANDS);

        DEFAULT_MODEL_ID = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "DefaultModelId",
                DEFAULT_MODEL_ID,
                "The default model ID when a player first enters the game")
            .getString();

        DEFAULT_MODEL_TEXTURE = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "DefaultModelTexture",
                DEFAULT_MODEL_TEXTURE,
                "The default model texture when a player first enters the game")
            .getString();

        // Extra player render config values
        DISABLE_PLAYER_RENDER = configuration
            .get("extra_player_render", "DisablePlayerRender", DISABLE_PLAYER_RENDER, "Whether to display player")
            .getBoolean(DISABLE_PLAYER_RENDER);

        PLAYER_POS_X = configuration
            .get("extra_player_render", "PlayerPosX", PLAYER_POS_X, "Player position x in screen", 0, Integer.MAX_VALUE)
            .getInt();

        PLAYER_POS_Y = configuration
            .get("extra_player_render", "PlayerPosY", PLAYER_POS_Y, "Player position y in screen", 0, Integer.MAX_VALUE)
            .getInt();

        PLAYER_SCALE = configuration
            .get("extra_player_render", "PlayerScale", PLAYER_SCALE, "Player scale in screen", 8.0, 360.0)
            .getDouble();

        PLAYER_YAW_OFFSET = configuration
            .get("extra_player_render", "PlayerYawOffset", PLAYER_YAW_OFFSET, "Player yaw offset in screen")
            .getDouble();

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
