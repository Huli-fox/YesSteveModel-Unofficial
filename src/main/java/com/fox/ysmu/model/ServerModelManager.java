package com.fox.ysmu.model;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

import org.apache.commons.io.FileUtils;

import com.fox.ysmu.data.EncryptTools;
import com.fox.ysmu.model.format.FolderFormat;
import com.fox.ysmu.model.format.OpenYsmFormat;
import com.fox.ysmu.model.format.ServerModelInfo;
import com.fox.ysmu.model.format.YsmFormat;
import com.fox.ysmu.model.resource.pojo.RawYsmModel;
import com.fox.ysmu.network.sync.ModelSyncServer17;
import com.fox.ysmu.util.GetJarResources;
import com.fox.ysmu.ysmu;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class ServerModelManager {

    /**
     * 配置相关文件夹
     */
    public static final Path FOLDER = Paths.get("config", ysmu.MODID);

    /**
     * 自定义模型所放置的文件夹
     */
    public static final Path BUILT = FOLDER.resolve("built");
    public static final Path CUSTOM = FOLDER.resolve("custom");
    public static final Path EXPORT = FOLDER.resolve("export");

    /**
     * 生成缓存文件的文件夹
     */
    public static final Path CACHE = FOLDER.resolve("cache");
    public static final Path CACHE_SERVER_INDEX_FILE = CACHE.resolve("server_index");
    public static final Path CACHE_SERVER = CACHE.resolve("server");
    /**
     * 存储密码的文件
     */
    public static final Path PASSWORD_FILE = CACHE_SERVER.resolve("PASSWORD");
    public static final Path CACHE_CLIENT = CACHE.resolve("client");
    /**
     * 模型内部 ID -> 模型额外信息缓存
     * 非安全磁盘名称会先编码为内部 ID，再写入此缓存。
     * 可以方便的通过此缓存，来判断客户端发来的 MD5 在不在服务端
     * 从而将服务器文件发送给玩家
     * 还可以获取其他服务端模型信息
     */
    public static final Map<String, ServerModelInfo> CACHE_NAME_INFO = Maps.newHashMap();
    public static final Map<String, RawYsmModel> RAW_MODEL_INFO = Maps.newHashMap();
    public static volatile byte[] OPEN_YSM_SERVER_KEY;

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
            ModelSyncServer17.requestSync(player);
        }
    }

    public static void sendRequestSyncModelMessage(EntityPlayer player) {
        ModelSyncServer17.requestSync(player);
    }

    public static void reloadPacks() {
        clearModelCaches();
        createConfigDirectories();
        copyBuiltInModels();
        initPassword();
        initOpenYsmServerIndex();
        initBuiltNoticeFile();
        initBlacklistFile();
        rebuildModelCaches();
    }

    private static void clearModelCaches() {
        CACHE_NAME_INFO.clear();
        RAW_MODEL_INFO.clear();
        ModelSyncServer17.clearModelCaches();
    }

    private static void createConfigDirectories() {
        createFolder(FOLDER);
        createFolder(BUILT);
        createFolder(CUSTOM);
        createFolder(EXPORT);

        createFolder(CACHE);
        createFolder(CACHE_SERVER);
        createFolder(CACHE_CLIENT);
    }

    private static void copyBuiltInModels() {
        // 不管存不存在，强行覆盖
        copyDefaultModel();
        copyWineFoxModel();
        copyVanillaModel();
    }

    private static void rebuildModelCaches() {
        OpenYsmFormat.cacheAllModels(BUILT);
        OpenYsmFormat.cacheAllModels(CUSTOM);
        cacheAllModels(CUSTOM);
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
        FolderFormat.cacheAllModels(rootPath);
    }

    private static void initPassword() {
        try {
            EncryptTools.createRandomPassword();
            File passwordFile = PASSWORD_FILE.toFile();
            if (passwordFile.isFile()) {
                boolean validPassword = EncryptTools.readPassword(FileUtils.readFileToByteArray(passwordFile));
                if (!validPassword) {
                    FileUtils.writeByteArrayToFile(passwordFile, EncryptTools.writePassword());
                }
            } else {
                FileUtils.writeByteArrayToFile(passwordFile, EncryptTools.writePassword());
            }
        } catch (Exception e) {
            ysmu.LOG.warn("Failed to initialize legacy model password", e);
        }
    }

    private static void initOpenYsmServerIndex() {
        try {
            byte[] serverKey = readOpenYsmServerKey();
            if (serverKey == null) {
                serverKey = new byte[56];
                new SecureRandom().nextBytes(serverKey);
                JsonObject root = new JsonObject();
                root.addProperty("server_key", Base64.getEncoder().encodeToString(serverKey));
                FileUtils.writeStringToFile(CACHE_SERVER_INDEX_FILE.toFile(), ysmu.GSON.toJson(root), StandardCharsets.UTF_8);
            }
            OPEN_YSM_SERVER_KEY = serverKey;
        } catch (Exception e) {
            ysmu.LOG.warn("Failed to initialize OpenYSM server_index", e);
            byte[] fallbackKey = new byte[56];
            new SecureRandom().nextBytes(fallbackKey);
            OPEN_YSM_SERVER_KEY = fallbackKey;
        }
    }

    private static byte[] readOpenYsmServerKey() {
        try {
            File serverIndexFile = CACHE_SERVER_INDEX_FILE.toFile();
            if (!serverIndexFile.isFile()) {
                return null;
            }
            String json = FileUtils.readFileToString(serverIndexFile, StandardCharsets.UTF_8);
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonElement serverKeyElement = root.get("server_key");
            if (serverKeyElement == null || !serverKeyElement.isJsonPrimitive()) {
                return null;
            }
            byte[] decoded = Base64.getDecoder().decode(serverKeyElement.getAsString());
            return decoded.length == 56 ? decoded : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void initBlacklistFile() {
        Path blacklistFile = FOLDER.resolve("blacklist.txt");
        if (Files.isRegularFile(blacklistFile)) {
            return;
        }
        try {
            String content = "# Yes Steve Model built-in model blacklist\n"
                + "# One Java regular expression per line. Lines starting with # are comments.\n"
                + "# The default legacy models copied into config/ysmu/custom are not controlled by this file yet.\n";
            FileUtils.writeStringToFile(blacklistFile.toFile(), content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            ysmu.LOG.warn("Failed to create OpenYSM blacklist file", e);
        }
    }

    private static void initBuiltNoticeFile() {
        Path noticeFile = BUILT.resolve("notice.txt");
        try {
            String content = "OpenYSM built-in model staging directory.\n"
                + "This phase creates the directory for the new scanner; legacy built-ins are still copied to custom.\n";
            FileUtils.writeStringToFile(noticeFile.toFile(), content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            ysmu.LOG.warn("Failed to write OpenYSM built directory notice", e);
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
                ysmu.LOG.warn("Failed to create directory {}", path, e);
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
