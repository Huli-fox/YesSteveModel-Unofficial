package com.fox.ysmu.client.gui;

import com.fox.ysmu.client.gui.button.FlatColorButton;
import com.fox.ysmu.compat.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

// TODO 需要分散进多个类
public class DownloadScreen extends GuiScreen {
    private final PlayerModelScreen parent;

    // --- 数据模型 ---
    private static class FileInfo {
        String name;
        String path;
        String category;
        String type;

        FileInfo(String path) {
            this.path = path;
            String[] parts = path.split("/");
            if (parts.length == 2) {
                this.category = parts[0];
                this.name = parts[1];
                this.type = this.name.endsWith(".zip") ? "zip" : "ysm";
            } else {
                // 不符合 "category/filename" 格式的数据将被忽略
                this.category = null;
                this.name = null;
                this.type = null;
            }
        }
    }

    // 这个静态内部类负责管理文件列表的缓存、加载状态和网络请求
    // 这样可以确保数据只加载一次，并且与GUI实例的生命周期解耦
    private static class FileListCache {
        private enum State { IDLE, LOADING, LOADED, ERROR }

        private static volatile State currentState = State.IDLE;
        private static volatile Map<String, List<FileInfo>> categorizedFiles = null;
        private static volatile String statusMessage = "";
        private static final String API_URL = "https://api.github.com/repos/Elaina69/Yes-Steve-Model-Repo/git/trees/main?recursive=1";

        public static synchronized void loadIfNeeded() {
            if (currentState == State.IDLE || currentState == State.ERROR) {
                fetchFileList();
            }
        }

        public static synchronized void forceLoad() {
            fetchFileList();
        }

        private static void fetchFileList() {
            if (currentState == State.LOADING) return;

            currentState = State.LOADING;
            statusMessage = "正在从GitHub加载文件列表...";

            new Thread(() -> {
                try {
                    URL url = new URL(API_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);

                    if (connection.getResponseCode() == 200) {
                        JsonParser parser = new JsonParser();
                        JsonElement element = parser.parse(new InputStreamReader(connection.getInputStream()));
                        JsonObject root = element.getAsJsonObject();
                        JsonArray tree = root.getAsJsonArray("tree");

                        Map<String, List<FileInfo>> tempMap = new TreeMap<>();
                        for (JsonElement fileElement : tree) {
                            JsonObject fileObject = fileElement.getAsJsonObject();
                            String path = fileObject.get("path").getAsString();
                            if (path.endsWith(".zip") || path.endsWith(".ysm")) {
                                FileInfo info = new FileInfo(path);
                                if (info.category != null) {
                                    tempMap.computeIfAbsent(info.category, k -> new ArrayList<>()).add(info);
                                }
                            }
                        }
                        categorizedFiles = tempMap;
                        statusMessage = EnumChatFormatting.GREEN + "文件列表加载成功!";
                        currentState = State.LOADED;
                    } else {
                        throw new RuntimeException("HTTP Error Code: " + connection.getResponseCode());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    categorizedFiles = null;
                    statusMessage = EnumChatFormatting.RED + "加载失败! " + e.getMessage();
                    currentState = State.ERROR;
                }
            }).start();
        }
    }


    // --- GUI 实例变量 ---
    private String selectedCategory = null;
    private String activeFilter = "zip";
    private FileInfo selectedFile = null;
    private int selectedCategoryIndex = -1;
    private int selectedFileIndex = -1;
    private String downloadStatusMessage = ""; // 用于显示下载进度的实例变量

    // GUI 组件
    private GuiButton downloadButton;
    private GuiButton zipFilterButton;
    private GuiButton ysmFilterButton;
    private CategoryList categoryListSlot;
    private FileList fileListSlot;

    // GUI 数据
    private final List<String> categoryNames = new ArrayList<>();
    private final List<FileInfo> filesForDisplay = new ArrayList<>();

    public DownloadScreen(PlayerModelScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int topMargin = 32;
        int bottomMargin = 32;
        int sideMargin = 20;

        // --- 按钮 ---
        int buttonHeight = 20;
        int topButtonY = 8;
        this.buttonList.add(new FlatColorButton(0, sideMargin, topButtonY, 80, buttonHeight, I18n.format("gui.yes_steve_model.model.return")));
        this.buttonList.add(new FlatColorButton(4, sideMargin + 84, topButtonY, 50, buttonHeight, "刷新")); // 新增刷新按钮

        int fileListX = sideMargin + 150 + 10;
        int fileListWidth = this.width - fileListX - sideMargin;

        int filterButtonWidth = 50;
        int filterButtonsX = fileListX + (fileListWidth / 2) - filterButtonWidth - 2;
        this.zipFilterButton = new GuiButton(1, filterButtonsX, topButtonY, filterButtonWidth, buttonHeight, "ZIP");
        this.ysmFilterButton = new GuiButton(2, filterButtonsX + filterButtonWidth + 4, topButtonY, filterButtonWidth, buttonHeight, "YSM");

        int downloadButtonWidth = 80;
        this.downloadButton = new GuiButton(3, this.width - downloadButtonWidth - sideMargin, topButtonY, downloadButtonWidth, buttonHeight, "下载");

        this.buttonList.add(zipFilterButton);
        this.buttonList.add(ysmFilterButton);
        this.buttonList.add(downloadButton);

        // --- 列表 ---
        int listHeight = this.height - topMargin - bottomMargin;
        this.categoryListSlot = new CategoryList(mc, 150, listHeight, topMargin, this.height - bottomMargin, sideMargin, 20);
        this.fileListSlot = new FileList(mc, fileListWidth, listHeight, topMargin, this.height - bottomMargin, fileListX, 30);

        // 触发加载
        FileListCache.loadIfNeeded();

        // 界面初始化时，根据缓存状态填充数据
        populateCategoriesFromCache();
        updateFileList();
        updateButtons();
    }

    // 从缓存中填充分类列表
    private void populateCategoriesFromCache() {
        if (FileListCache.currentState == FileListCache.State.LOADED && FileListCache.categorizedFiles != null) {
            this.categoryNames.clear();
            this.categoryNames.addAll(FileListCache.categorizedFiles.keySet());
            // 排序可以放到缓存加载成功时做一次即可
            Collections.sort(this.categoryNames);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // 在updateScreen中检查加载状态的变化，如果刚加载完，则刷新列表
        if (FileListCache.currentState == FileListCache.State.LOADED && this.categoryNames.isEmpty()) {
            populateCategoriesFromCache();
        }
    }

    private void updateFileList() {
        this.filesForDisplay.clear();
        this.selectedFile = null;
        this.selectedFileIndex = -1;

        if (selectedCategory != null && FileListCache.categorizedFiles != null) {
            List<FileInfo> files = FileListCache.categorizedFiles.get(selectedCategory);
            if (files != null) {
                for (FileInfo file : files) {
                    if (file.type.equals(activeFilter)) {
                        this.filesForDisplay.add(file);
                    }
                }
            }
        }
        if (this.fileListSlot != null) {
            this.fileListSlot.scrollBy(-this.fileListSlot.getAmountScrolled());
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean categorySelected = this.selectedCategory != null;
        this.zipFilterButton.visible = categorySelected;
        this.ysmFilterButton.visible = categorySelected;
        this.zipFilterButton.enabled = !("zip".equals(activeFilter) && categorySelected);
        this.ysmFilterButton.enabled = !("ysm".equals(activeFilter) && categorySelected);
        this.downloadButton.enabled = this.selectedFile != null;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;
        switch (button.id) {
            case 0: // 返回
                this.mc.displayGuiScreen(parent);
                break;
            case 1: // ZIP 筛选
                this.activeFilter = "zip";
                updateFileList();
                break;
            case 2: // YSM 筛选
                this.activeFilter = "ysm";
                updateFileList();
                break;
            case 3: // 下载
                if (this.selectedFile != null) {
                    downloadFile(this.selectedFile, new File(this.mc.mcDataDir, "config" + File.separator + "ysmu" + File.separator + "custom"));
                }
                break;
            case 4: // 刷新
                FileListCache.forceLoad();
                // 清空当前显示，等待重新加载
                this.categoryNames.clear();
                this.selectedCategory = null;
                this.selectedCategoryIndex = -1;
                updateFileList();
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        // 根据缓存状态绘制界面
        switch (FileListCache.currentState) {
            case LOADED:
                if (this.categoryListSlot != null) this.categoryListSlot.drawScreen(mouseX, mouseY, partialTicks);
                if (this.fileListSlot != null) this.fileListSlot.drawScreen(mouseX, mouseY, partialTicks);
                break;
            case LOADING:
                this.drawCenteredString(this.fontRendererObj, FileListCache.statusMessage, this.width / 2, this.height / 2, 0xFFFFFF);
                break;
            case ERROR:
                this.drawCenteredString(this.fontRendererObj, EnumChatFormatting.RED + "加载失败! 请检查网络或点击“刷新”重试。", this.width / 2, this.height / 2, 0xFFFFFF);
                break;
            case IDLE:
                // 正常情况下不会停留在 IDLE 状态
                this.drawCenteredString(this.fontRendererObj, "...", this.width / 2, this.height / 2, 0xFFFFFF);
                break;
        }

        // 绘制所有按钮
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 绘制底部的状态信息
        String statusToDraw = this.downloadStatusMessage;
        if (statusToDraw.isEmpty()) {
            statusToDraw = FileListCache.statusMessage;
        }
        this.drawCenteredString(this.fontRendererObj, statusToDraw, this.width / 2, this.height - 20, 0xFFFFFF);
    }

    abstract class ScissoredGuiSlot extends GuiSlot {
        public ScissoredGuiSlot(Minecraft mc, int width, int height, int top, int bottom, int left, int slotHeight) {
            super(mc, width, height, top, bottom, slotHeight);
            this.left = left;
            this.right = left + width;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            int scale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
            int scissorX = this.left * scale;
            int scissorY = mc.displayHeight - this.bottom * scale;
            int scissorW = this.width * scale;
            int scissorH = (this.bottom - this.top) * scale;
            GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
            super.drawScreen(mouseX, mouseY, partialTicks);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        @Override
        public int getListWidth() { return this.width; }

        @Override
        protected int getScrollBarX() { return this.right - 6; }

        @Override
        protected void drawBackground() {}
    }

    class CategoryList extends ScissoredGuiSlot {
        public CategoryList(Minecraft mc, int width, int height, int top, int bottom, int left, int slotHeight) {
            super(mc, width, height, top, bottom, left, slotHeight);
        }

        @Override
        protected int getSize() { return categoryNames.size(); }

        @Override
        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
            if (slotIndex >= 0 && slotIndex < categoryNames.size()) {
                selectedCategoryIndex = slotIndex;
                selectedCategory = categoryNames.get(slotIndex);
                updateFileList();
            }
        }

        @Override
        protected boolean isSelected(int slotIndex) { return slotIndex == selectedCategoryIndex; }

        @Override
        protected void drawSlot(int slotIndex, int xPos, int yPos, int height, Tessellator tessellator, int mouseX, int mouseY) {
            if (slotIndex >= 0 && slotIndex < categoryNames.size()) {
                String name = categoryNames.get(slotIndex);
                fontRendererObj.drawString(fontRendererObj.trimStringToWidth(name, this.width - 10), this.left + 5, yPos + 5, 0xFFFFFF);
            }
        }
    }

    class FileList extends ScissoredGuiSlot {
        public FileList(Minecraft mc, int width, int height, int top, int bottom, int left, int slotHeight) {
            super(mc, width, height, top, bottom, left, slotHeight);
        }

        @Override
        protected int getSize() { return filesForDisplay.size(); }

        @Override
        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
            if (slotIndex >= 0 && slotIndex < filesForDisplay.size()) {
                selectedFileIndex = slotIndex;
                selectedFile = filesForDisplay.get(slotIndex);
                updateButtons();
            }
        }

        @Override
        protected boolean isSelected(int slotIndex) { return slotIndex == selectedFileIndex; }

        @Override
        protected void drawSlot(int slotIndex, int xPos, int yPos, int height, Tessellator tessellator, int mouseX, int mouseY) {
            if (slotIndex >= 0 && slotIndex < filesForDisplay.size()) {
                FileInfo file = filesForDisplay.get(slotIndex);
                fontRendererObj.drawString(fontRendererObj.trimStringToWidth(file.name, this.width - 10), this.left + 5, yPos + 8, 0xFFFFFF);
            }
        }
    }

    // --- 文件下载逻辑 ---
    private void downloadFile(final FileInfo fileInfo, final File downloadDir) {
        new Thread(() -> {
            try {
                if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                    throw new RuntimeException("无法创建下载目录: " + downloadDir.getAbsolutePath());
                }
                File destination = new File(downloadDir, fileInfo.name);
                downloadStatusMessage = "开始下载: " + fileInfo.name;

                String baseUrl = "https://raw.githubusercontent.com/Elaina69/Yes-Steve-Model-Repo/main/";
                String[] pathParts = fileInfo.path.split("/");
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < pathParts.length; i++) {
                    builder.append(java.net.URLEncoder.encode(pathParts[i], "UTF-8").replace("+", "%20"));
                    if (i < pathParts.length - 1) {
                        builder.append("/");
                    }
                }
                URL url = new URL(baseUrl + builder);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                long fileSize = connection.getContentLengthLong();

                try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(destination)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytesRead = 0;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        if (fileSize > 0) {
                            int progress = (int) ((totalBytesRead * 100) / fileSize);
                            downloadStatusMessage = "正在下载: " + fileInfo.name + " (" + progress + "%)";
                        } else {
                            downloadStatusMessage = "正在下载: " + fileInfo.name + " (" + (totalBytesRead / 1024) + " KB)";
                        }
                    }
                }
                downloadStatusMessage = EnumChatFormatting.GREEN + "下载完成: " + fileInfo.name;
                if ("zip".equals(fileInfo.type)) {
                    downloadStatusMessage = "正在解压: " + fileInfo.name;
                    Utils.unzip(destination, downloadDir);
                    downloadStatusMessage = EnumChatFormatting.GREEN + "解压完成: " + fileInfo.name;
                    // 解压成功后删除原ZIP文件
                    if (!destination.delete()) {
                        System.out.println("Warning: Failed to delete zip file after extraction: " + destination.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                downloadStatusMessage = EnumChatFormatting.RED + "下载失败: " + fileInfo.name;
            }
        }).start();
    }
}
