package com.fox.ysmu.compat;

import com.github.promeg.pinyinhelper.Pinyin;
import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class YsmConverter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public enum Version {
        V_1_2_0, V_1_1_5, V_1_1_4, UNKNOWN
    }

    /**
     * 转换并替换方法
     * 将 sourceDir 转换为 1.1.4 规范，并处理文件替换逻辑
     *
     * @param sourceFolder 原始模型文件夹
     * @param parentDir    CUSTOM 根目录
     */
    public static void convertAndReplace(File sourceFolder, File parentDir) throws IOException {
        Path srcPath = sourceFolder.toPath();
        Path parentPath = parentDir.toPath();

        Version currentVersion = detectVersion(srcPath);

        // 1. 获取规范化后的名字
        JsonObject metadata = extractMetadata(srcPath, currentVersion);
        String packName = sourceFolder.getName();
        String safeName = sanitizeDirName(packName);

        // 目标路径
        Path finalDestPath = parentPath.resolve(safeName);

        // 特殊情况：如果当前就是 1.1.4 且名字已经规范，直接跳过
        if (currentVersion == Version.V_1_1_4 && packName.equals(safeName)) {
            return;
        }

        // 2. 创建一个临时目录用于存放生成的文件，防止正在读取时删除出错
        // 命名规则：.temp_convert_名字_时间戳
        String tempDirName = ".temp_convert_" + safeName + "_" + System.currentTimeMillis();
        Path tempPath = parentPath.resolve(tempDirName);
        Files.createDirectories(tempPath);

        try {
            // 3. 执行转换逻辑，输出到临时目录
            convertTo114(srcPath, tempPath, metadata, currentVersion);

            // 4. 转换成功后，删除原始文件夹 (递归删除)
            deleteDirectoryRecursively(srcPath);

            // 5. 如果目标路径(finalDestPath)已存在 (例如 old_name 转换后变成了 existing_name)
            // 此时也需要删除已存在的目标，或者进行合并。这里选择覆盖模式：删除旧的
            if (Files.exists(finalDestPath)) {
                 deleteDirectoryRecursively(finalDestPath);
            }

            // 6. 将临时目录重命名为最终目录
            Files.move(tempPath, finalDestPath, StandardCopyOption.ATOMIC_MOVE);

        } catch (Exception e) {
            // 如果出错，清理临时目录
            deleteDirectoryRecursively(tempPath);
            throw new IOException("转换过程出错", e);
        }
    }

    /**
     * 核心转换逻辑 (内部调用)
     */
    private static void convertTo114(Path srcPath, Path destPath, JsonObject metadata, Version currentVersion) throws IOException {
        if (currentVersion == Version.UNKNOWN) {
            // 尝试即使是未知结构也复制 png，防止彻底丢失，但这里严格一点抛出异常
            // 或者你可以选择只复制 png
            throw new IOException("未识别的模型包版本结构");
        }

        // 处理 main.json (注入元数据)
        Path mainJsonSrc = locateFile(srcPath, currentVersion, "main.json", "models");
        if (Files.exists(mainJsonSrc)) {
            processMainJson(mainJsonSrc, destPath.resolve("main.json"), metadata);
        }

        // 复制其他白名单文件
        copyAllowedFile(srcPath, destPath, currentVersion, "arm.json", "models");
        copyAllowedFile(srcPath, destPath, currentVersion, "main.animation.json", "animations");
        copyAllowedFile(srcPath, destPath, currentVersion, "arm.animation.json", "animations");
        copyAllowedFile(srcPath, destPath, currentVersion, "extra.animation.json", "animations");

        // 复制所有贴图 (.png)
        copyTextures(srcPath, destPath, currentVersion);
    }

    /**
     * 规范化文件夹名称 (公开供外部调用)
     */
    public static String sanitizeDirName(String name) {
        if (name == null || name.isEmpty()) {
            return "convertedmodel";
        }
        // 中文转拼音
        String pinyinStr = Pinyin.toPinyin(name, "");
        // 转小写
        String lowerCaseName = pinyinStr.toLowerCase();
        String validName = lowerCaseName.replaceAll("[^a-z0-9]", "");
        return validName.isEmpty() ? "convertedmodel" : validName;
    }

    private static Version detectVersion(Path path) {
        if (Files.exists(path.resolve("ysm.json"))) return Version.V_1_2_0;
        if (Files.exists(path.resolve("info.json"))) return Version.V_1_1_5;
        if (Files.exists(path.resolve("main.json")) && !Files.exists(path.resolve("info.json"))) return Version.V_1_1_4;
        return Version.UNKNOWN;
    }

    private static JsonObject extractMetadata(Path srcPath, Version version) throws IOException {
        JsonObject rawInfo = new JsonObject();
        File configFile = version == Version.V_1_2_0 ? srcPath.resolve("ysm.json").toFile() : srcPath.resolve("info.json").toFile();

        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                rawInfo = new JsonParser().parse(reader).getAsJsonObject();
            }
        } else if (version == Version.V_1_1_4) {
            // 如果是 1.1.4，尝试从 main.json 读取现有的 extra info 以便保留
             Path mainJson = srcPath.resolve("main.json");
             if(Files.exists(mainJson)) {
                 try (Reader reader = new FileReader(mainJson.toFile())) {
                     JsonObject main = new JsonParser().parse(reader).getAsJsonObject();
                     if(main.has("minecraft:geometry")) {
                         JsonArray geo = main.getAsJsonArray("minecraft:geometry");
                         if(geo.size() > 0 && geo.get(0).getAsJsonObject().has("description")) {
                             JsonObject desc = geo.get(0).getAsJsonObject().getAsJsonObject("description");
                             if(desc.has("ysm_extra_info")) return desc.getAsJsonObject("ysm_extra_info");
                         }
                     }
                 }
             }
             // 如果都没找到，返回空对象，依靠 sanitizeDirName 使用文件夹名
             return rawInfo;
        }

        // 构建 1.1.4 格式的 ysm_extra_info
        JsonObject extraInfo = new JsonObject();

        if (version == Version.V_1_2_0) {
            // 1.2.0 ysm.json 结构转换
            if (rawInfo.has("metadata")) {
                JsonObject md = rawInfo.getAsJsonObject("metadata");
                if (md.has("name")) extraInfo.add("name", md.get("name"));
                if (md.has("tips")) extraInfo.add("tips", md.get("tips"));

                // License
                if (md.has("license")) {
                    JsonElement lic = md.get("license");
                    if (lic.isJsonObject() && lic.getAsJsonObject().has("type")) {
                        extraInfo.add("license", lic.getAsJsonObject().get("type"));
                    }
                }

                // Authors (转为纯字符串数组)
                if (md.has("authors")) {
                    JsonArray srcAuthors = md.getAsJsonArray("authors");
                    JsonArray destAuthors = new JsonArray();
                    for (JsonElement e : srcAuthors) {
                        if (e.isJsonObject() && e.getAsJsonObject().has("name")) {
                            destAuthors.add(e.getAsJsonObject().get("name"));
                        }
                    }
                    extraInfo.add("authors", destAuthors);
                }
            }

            // 1.2.0 properties (height_scale等) 需要特殊处理，这里暂时只处理 info
            // Extra Animations (1.2.0 是 Map "extra0":"name", 1.1.4 是 List)
            if (rawInfo.has("properties") && rawInfo.getAsJsonObject("properties").has("extra_animation")) {
                JsonObject extras = rawInfo.getAsJsonObject("properties").getAsJsonObject("extra_animation");
                JsonArray names = new JsonArray();
                // 简单遍历 map values
                for (Map.Entry<String, JsonElement> entry : extras.entrySet()) {
                    names.add(entry.getValue());
                }
                extraInfo.add("extra_animation_names", names);
            }

        } else {
            // 1.1.5 info.json 结构与 1.1.4 嵌入的结构基本一致，直接复制关键字段
            if (rawInfo.has("name")) extraInfo.add("name", rawInfo.get("name"));
            if (rawInfo.has("authors")) extraInfo.add("authors", rawInfo.get("authors"));
            if (rawInfo.has("tips")) extraInfo.add("tips", rawInfo.get("tips"));
            if (rawInfo.has("license")) extraInfo.add("license", rawInfo.get("license"));
            if (rawInfo.has("extra_animation_names")) extraInfo.add("extra_animation_names", rawInfo.get("extra_animation_names"));
        }

        // 暂时存储 scale 信息以便后续注入，不放入 ysm_extra_info
        if (version == Version.V_1_2_0 && rawInfo.has("properties")) {
            JsonObject props = rawInfo.getAsJsonObject("properties");
            if (props.has("height_scale")) extraInfo.addProperty("__temp_h_scale", props.get("height_scale").getAsDouble());
            if (props.has("width_scale")) extraInfo.addProperty("__temp_w_scale", props.get("width_scale").getAsDouble());
        }

        return extraInfo;
    }

    /**
     * 读取 main.json，注入 ysm_extra_info，并写入新文件
     */
    private static void processMainJson(Path srcFile, Path destFile, JsonObject metaData) throws IOException {
        JsonObject mainJson;
        try (Reader reader = new FileReader(srcFile.toFile())) {
            mainJson = new JsonParser().parse(reader).getAsJsonObject();
        }

        // 提取并移除临时存储的 scale 数据
        Double hScale = metaData.has("__temp_h_scale") ? metaData.remove("__temp_h_scale").getAsDouble() : null;
        Double wScale = metaData.has("__temp_w_scale") ? metaData.remove("__temp_w_scale").getAsDouble() : null;

        // 定位到 minecraft:geometry -> [0] -> description
        if (mainJson.has("minecraft:geometry")) {
            JsonArray geoArray = mainJson.getAsJsonArray("minecraft:geometry");
            if (geoArray.size() > 0) {
                JsonObject geoObj = geoArray.get(0).getAsJsonObject();
                if (!geoObj.has("description")) {
                    geoObj.add("description", new JsonObject());
                }
                JsonObject description = geoObj.getAsJsonObject("description");

                // 注入 ysm_extra_info
                description.add("ysm_extra_info", metaData);

                // 注入 scale (1.1.4 直接在 description 下)
                if (hScale != null) description.addProperty("ysm_height_scale", hScale);
                if (wScale != null) description.addProperty("ysm_width_scale", wScale);
            }
        }

        try (Writer writer = new FileWriter(destFile.toFile())) {
            GSON.toJson(mainJson, writer);
        }
    }

    private static Path locateFile(Path root, Version version, String fileName, String subFolder120) {
        if (version == Version.V_1_2_0) {
            // 1.2.0 在子文件夹中
            Path p = root.resolve(subFolder120).resolve(fileName);
            if (Files.exists(p)) return p;
            // 容错：有时可能不在子文件夹
            if (Files.exists(root.resolve(fileName))) return root.resolve(fileName);
        } else {
            // 1.1.5 在根目录
            return root.resolve(fileName);
        }
        return root.resolve(fileName);
    }

    private static void copyAllowedFile(Path srcRoot, Path destRoot, Version version, String fileName, String subFolder120) throws IOException {
        Path srcFile = locateFile(srcRoot, version, fileName, subFolder120);
        if (Files.exists(srcFile)) {
            Files.copy(srcFile, destRoot.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copyTextures(Path srcRoot, Path destRoot, Version version) throws IOException {
        // 确定贴图源目录
        Path textureDir = srcRoot;
        if (version == Version.V_1_2_0 && Files.isDirectory(srcRoot.resolve("textures"))) {
            textureDir = srcRoot.resolve("textures");
        }

        if (!Files.isDirectory(textureDir)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(textureDir, "*.png")) {
            for (Path entry : stream) {
                if (entry.getFileName().toString().toLowerCase().contains("arrow")) {
                    continue;
                }
                // 1.1.4 都在根目录
                Files.copy(entry, destRoot.resolve(entry.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * 递归删除目录的辅助方法 (Java NIO)
     */
    private static void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
