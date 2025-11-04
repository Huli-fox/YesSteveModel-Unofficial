package com.fox.ysmu.model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;

import org.apache.commons.io.FileUtils;

import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.data.EncryptTools;
import com.fox.ysmu.model.format.FolderFormat;
import com.fox.ysmu.model.format.ServerModelInfo;
import com.fox.ysmu.model.format.YsmFormat;
import com.fox.ysmu.model.format.ZipFormat;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.RequestSyncModel;
import com.fox.ysmu.util.GetJarResources;
import com.fox.ysmu.ysmu;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public final class ServerModelManager {

    /**
     * 配置相关文件夹
     */
    public static final Path FOLDER = Paths.get("config", ysmu.MODID);

    /**
     * 自定义模型所放置的文件夹
     */
    public static final Path CUSTOM = FOLDER.resolve("custom");
    public static final Path AUTH = FOLDER.resolve("auth");
    public static final Path EXPORT = FOLDER.resolve("export");

    /**
     * 生成缓存文件的文件夹
     */
    public static final Path CACHE = FOLDER.resolve("cache");
    public static final Path CACHE_SERVER = CACHE.resolve("server");
    /**
     * 存储密码的文件
     */
    public static final Path PASSWORD_FILE = CACHE_SERVER.resolve("PASSWORD");
    public static final Path CACHE_CLIENT = CACHE.resolve("client");
    /**
     * 模型名称 -> 模型额外信息缓存
     * 可以方便的通过此缓存，来判断客户端发来的 MD5 在不在服务端
     * 从而将服务器文件发送给玩家
     * 还可以获取其他服务端模型信息
     */
    public static final Map<String, ServerModelInfo> CACHE_NAME_INFO = Maps.newHashMap();

    /**
     * 放置授权模型名称
     */
    public static final Set<String> AUTH_MODELS = Sets.newHashSet();

    /**
     * 特定文件名
     */
    public static final String MAIN_MODEL_FILE_NAME = "main.json";
    public static final String ARM_MODEL_FILE_NAME = "arm.json";
    public static final String MAIN_ANIMATION_FILE_NAME = "main.animation.json";
    public static final String ARM_ANIMATION_FILE_NAME = "arm.animation.json";
    public static final String EXTRA_ANIMATION_FILE_NAME = "extra.animation.json";

    public static void sendRequestSyncModelMessage(List<EntityPlayer> playerList) {
        for (EntityPlayer player : playerList) {
            NetworkHandler.sendToClientPlayer(new RequestSyncModel(), player);
        }
    }

    public static void sendRequestSyncModelMessage() {
        ClientModelManager.sendSyncModelMessage();
    }

    public static void sendRequestSyncModelMessage(EntityPlayer player) {
        NetworkHandler.sendToClientPlayer(new RequestSyncModel(), player);
    }

    public static void reloadPacks() {
        CACHE_NAME_INFO.clear();
        AUTH_MODELS.clear();

        createFolder(FOLDER);
        createFolder(CUSTOM);
        createFolder(AUTH);
        createFolder(EXPORT);

        createFolder(CACHE);
        createFolder(CACHE_SERVER);
        createFolder(CACHE_CLIENT);

        // 不管存不存在，强行覆盖
        copyDefaultModel();
        copyWineFoxModel();
        copyVanillaModel();
        initPassword();
        cacheAllModels(CUSTOM);
        cacheAllModels(AUTH);
    }

    private static void copyDefaultModel() {
        Path defaultPath = CUSTOM.resolve("default");
        createFolder(defaultPath);

        GetJarResources
            .copyYesSteveModelFile(getCustomFiles("custom/default/main.json"), defaultPath, MAIN_MODEL_FILE_NAME);
        GetJarResources
            .copyYesSteveModelFile(getCustomFiles("custom/default/arm.json"), defaultPath, ARM_MODEL_FILE_NAME);
        GetJarResources.copyYesSteveModelFile(getCustomFiles("custom/default/default.png"), defaultPath, "default.png");
        GetJarResources.copyYesSteveModelFile(getCustomFiles("custom/default/blue.png"), defaultPath, "blue.png");
        GetJarResources.copyYesSteveModelFile(
            getCustomFiles("custom/default/main.animation.json"),
            defaultPath,
            MAIN_ANIMATION_FILE_NAME);
        GetJarResources.copyYesSteveModelFile(
            getCustomFiles("custom/default/arm.animation.json"),
            defaultPath,
            ARM_ANIMATION_FILE_NAME);
        GetJarResources.copyYesSteveModelFile(
            getCustomFiles("custom/default/extra.animation.json"),
            defaultPath,
            EXTRA_ANIMATION_FILE_NAME);

        Path defaultBoyPath = CUSTOM.resolve("default_boy");
        createFolder(defaultBoyPath);

        GetJarResources.copyYesSteveModelFile(
            getCustomFiles("custom/default_boy/main.json"),
            defaultBoyPath,
            MAIN_MODEL_FILE_NAME);
        GetJarResources
            .copyYesSteveModelFile(getCustomFiles("custom/default_boy/arm.json"), defaultBoyPath, ARM_MODEL_FILE_NAME);
        GetJarResources.copyYesSteveModelFile(getCustomFiles("custom/default_boy/red.png"), defaultBoyPath, "red.png");
        GetJarResources
            .copyYesSteveModelFile(getCustomFiles("custom/default_boy/blue.png"), defaultBoyPath, "blue.png");
        GetJarResources.copyYesSteveModelFile(
            getCustomFiles("custom/default_boy/main.animation.json"),
            defaultBoyPath,
            MAIN_ANIMATION_FILE_NAME);
    }

    private static void copyVanillaModel() {
        Path stevePath = CUSTOM.resolve("steve");
        createFolder(stevePath);
        GetJarResources
            .copyYesSteveModelFile(getCustomFiles("custom/steve/main.json"), stevePath, MAIN_MODEL_FILE_NAME);
        GetJarResources.copyYesSteveModelFile(getCustomFiles("custom/steve/arm.json"), stevePath, ARM_MODEL_FILE_NAME);
        GetJarResources
            .copyYesSteveModelFile(getCustomFiles("custom/steve/tartaric_acid.png"), stevePath, "tartaric_acid.png");
        GetJarResources.copyYesSteveModelFile(
            getCustomFiles("custom/steve/main.animation.json"),
            stevePath,
            MAIN_ANIMATION_FILE_NAME);

        Path alexPath = CUSTOM.resolve("alex");
        createFolder(alexPath);
        GetJarResources.copyYesSteveModelFile(getCustomFiles("custom/alex/main.json"), alexPath, MAIN_MODEL_FILE_NAME);
        GetJarResources.copyYesSteveModelFile(getCustomFiles("custom/alex/arm.json"), alexPath, ARM_MODEL_FILE_NAME);
        GetJarResources.copyYesSteveModelFile(getCustomFiles("custom/alex/gsl.png"), alexPath, "gsl.png");
        GetJarResources.copyYesSteveModelFile(
            getCustomFiles("custom/alex/main.animation.json"),
            alexPath,
            MAIN_ANIMATION_FILE_NAME);

        Path qinglukaPath = CUSTOM.resolve("qingluka");
        createFolder(qinglukaPath);
        GetJarResources
            .copyYesSteveModelFile(getCustomFiles("custom/qingluka/main.json"), qinglukaPath, MAIN_MODEL_FILE_NAME);
        GetJarResources
            .copyYesSteveModelFile(getCustomFiles("custom/qingluka/arm.json"), qinglukaPath, ARM_MODEL_FILE_NAME);
        GetJarResources
            .copyYesSteveModelFile(getCustomFiles("custom/qingluka/texture.png"), qinglukaPath, "texture.png");
    }

    private static void copyWineFoxModel() {
        Path wineFoxPath = CUSTOM.resolve("wine_fox");
        createFolder(wineFoxPath);

        GetJarResources
            .copyYesSteveModelFile(getCustomFiles("custom/wine_fox/main.json"), wineFoxPath, MAIN_MODEL_FILE_NAME);
        GetJarResources
            .copyYesSteveModelFile(getCustomFiles("custom/wine_fox/arm.json"), wineFoxPath, ARM_MODEL_FILE_NAME);
        GetJarResources.copyYesSteveModelFile(getCustomFiles("custom/wine_fox/skin.png"), wineFoxPath, "skin.png");
        GetJarResources.copyYesSteveModelFile(
            getCustomFiles("custom/wine_fox/main.animation.json"),
            wineFoxPath,
            MAIN_ANIMATION_FILE_NAME);
    }

    private static void cacheAllModels(Path rootPath) {
        YsmFormat.cacheAllModels(rootPath);
        ZipFormat.cacheAllModels(rootPath);
        FolderFormat.cacheAllModels(rootPath);
    }

    private static void initPassword() {
        try {
            EncryptTools.createRandomPassword();
            File passwordFile = PASSWORD_FILE.toFile();
            if (passwordFile.isFile()) {
                EncryptTools.readPassword(FileUtils.readFileToByteArray(passwordFile));
            } else {
                FileUtils.writeByteArrayToFile(passwordFile, EncryptTools.writePassword());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getCustomFiles(String path) {
        return String.format("/assets/%s/%s", ysmu.MODID, path);
    }

    private static void createFolder(Path path) {
        File folder = path.toFile();
        if (!folder.isDirectory()) {
            try {
                Files.createDirectories(folder.toPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String removeExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex != -1) {
            fileName = fileName.substring(0, lastIndex);
        }
        return fileName;
    }
}
