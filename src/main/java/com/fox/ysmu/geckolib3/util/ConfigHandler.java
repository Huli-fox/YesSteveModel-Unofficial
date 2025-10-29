package com.fox.ysmu.geckolib3.util;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class ConfigHandler {
    public static Configuration config;
    public static boolean enableExamples;
    public static boolean debugPrintStacktraces;

    public ConfigHandler() {
    }

    public static void init(File file) {
        config = new Configuration(file);
        syncConfig();
    }

    public static void load() {
        config.load();
    }

    public static void syncConfig() {
        config.addCustomCategoryComment("general", "General Settings");
        enableExamples = config.getBoolean("enableexamples", "general", false, "Enable Geckolib Examples");
        debugPrintStacktraces = config.getBoolean("debugStacktraces", "general", false, "Print stacktraces for debugging");
        config.save();
    }
}
