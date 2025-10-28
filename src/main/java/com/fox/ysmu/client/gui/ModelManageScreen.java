package com.fox.ysmu.client.gui;

import com.fox.ysmu.client.gui.button.FlatColorButton;
import com.fox.ysmu.client.gui.button.ModelInfoButton;
import com.fox.ysmu.client.upload.UploadManager;
import com.fox.ysmu.model.format.Type;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.HandleFile;
import com.fox.ysmu.network.message.RefreshModelManage;
import com.fox.ysmu.network.message.RequestServerModelInfo;
import com.fox.ysmu.network.message.UploadFile;
import com.fox.ysmu.util.Keep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public class ModelManageScreen extends Screen {
    private static final int MAX_COUNT = 13;
    private final List<RequestServerModelInfo.Info> customModels;
    private final List<RequestServerModelInfo.Info> authModels;
    private volatile MutableComponent uploadError = null;
    private EditBox textField;
    private static boolean isCustomModels = true;
    private int index = -1;
    private Action action = Action.EMPTY;
    private static int page = 0;
    private int modelsCount = 0;
    private int x;
    private int y;

    public ModelManageScreen(List<RequestServerModelInfo.Info> customModels, List<RequestServerModelInfo.Info> authModels) {
        super(Component.literal("Model Manage Screen"));
        this.customModels = customModels;
        this.authModels = authModels;
    }

    private void calculateList() {
        this.modelsCount = getModels().size();
        if ((this.modelsCount - 1) / MAX_COUNT < page) {
            page = 0;
        }
    }

    @Override
    @Keep
    protected void init() {
        this.clearWidgets();
        this.calculateList();
        this.x = (width - 420) / 2;
        this.y = (height - 235) / 2;
        this.addTopButtons();
        this.addPageButtons();
        this.addModelInfoButtons();
        if (index >= 0) {
            addActionButtons();
        }
        if (this.action != Action.EMPTY) {
            addExtraButtons();
        }
    }

    private void addExtraButtons() {
        if (this.action != Action.UPLOAD || StringUtils.isNoneBlank(UploadManager.FILE_PATH)) {
            addRenderableWidget(new FlatColorButton(x + 270, y + 235 - 23, 70, 18, Component.translatable("gui.yes_steve_model.model_manage.confirm"), (b) -> {
                boolean canConfirm = false;
                if (index >= 0 && index < getModels().size()) {
                    RequestServerModelInfo.Info info = getModels().get(index);
                    UploadFile.Dir dir = isCustomModels ? UploadFile.Dir.CUSTOM : UploadFile.Dir.AUTH;
                    if (this.action == Action.DELETE) {
                        NetworkHandler.CHANNEL.sendToServer(new HandleFile(info.getFileName(), dir, "delete", ""));
                        canConfirm = true;
                    }
                    if (this.action == Action.MOVE) {
                        NetworkHandler.CHANNEL.sendToServer(new HandleFile(info.getFileName(), dir, "move", ""));
                        canConfirm = true;
                    }
                    if (this.action == Action.RENAME && StringUtils.isNotBlank(this.textField.getValue())) {
                        String value = this.textField.getValue();
                        String fileName = info.getFileName();
                        if (info.getType() == Type.FOLDER && !value.equals(fileName)) {
                            NetworkHandler.CHANNEL.sendToServer(new HandleFile(info.getFileName(), dir, "rename", value));
                            canConfirm = true;
                        }
                        if (info.getType() != Type.FOLDER && !value.equals(fileName.substring(0, fileName.length() - 4))) {
                            value = value + fileName.substring(fileName.length() - 4);
                            NetworkHandler.CHANNEL.sendToServer(new HandleFile(info.getFileName(), dir, "rename", value));
                            canConfirm = true;
                        }
                    }
                }
                if (this.action == Action.UPLOAD && StringUtils.isNoneBlank(UploadManager.FILE_PATH) && UploadManager.STATUE == UploadManager.Statue.FULFILL) {
                    try {
                        uploadFile(UploadManager.FILE_PATH, isCustomModels);
                        canConfirm = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (canConfirm) {
                    this.action = Action.EMPTY;
                    NetworkHandler.CHANNEL.sendToServer(new RefreshModelManage());
                }
            }));
            addRenderableWidget(new FlatColorButton(x + 345, y + 235 - 23, 70, 18, Component.translatable("gui.yes_steve_model.model_manage.cancel"), (b) -> {
                this.action = Action.EMPTY;
                this.init();
            }));
        }
        if (this.action == Action.RENAME) {
            textField = new EditBox(getMinecraft().font, x + 270, y + 51, 145, 14, Component.literal("YSM Rename Box"));
            textField.setTextColor(0xF3EFE0);
            textField.setMaxLength(24);
            textField.moveCursorToEnd();
            this.addWidget(this.textField);
        }
    }

    private void addActionButtons() {
        addRenderableWidget(new FlatColorButton(x + 5, y + 235 - 23, 80, 18, Component.translatable("gui.yes_steve_model.model_manage.delete"), (b) -> {
            this.action = Action.DELETE;
            this.init();
        }));
        addRenderableWidget(new FlatColorButton(x + 90, y + 235 - 23, 80, 18, Component.translatable("gui.yes_steve_model.model_manage.move"), (b) -> {
            this.action = Action.MOVE;
            this.init();
        }));
        addRenderableWidget(new FlatColorButton(x + 175, y + 235 - 23, 80, 18, Component.translatable("gui.yes_steve_model.model_manage.rename"), (b) -> {
            this.action = Action.RENAME;
            this.init();
        }));
    }

    private void addModelInfoButtons() {
        int modelsY = y + 51;
        int count = page * MAX_COUNT;
        for (int i = count; i < count + MAX_COUNT; i++) {
            if (i >= this.getModels().size()) {
                return;
            }
            RequestServerModelInfo.Info info = this.getModels().get(i);
            final int finalIndex = i;
            ModelInfoButton modelInfoButton = new ModelInfoButton(x + 5, modelsY, 12, info, (b) -> {
                this.index = finalIndex;
                this.action = Action.EMPTY;
                this.init();
            });
            if (this.index == i) {
                modelInfoButton.setSelect(true);
            }
            addRenderableWidget(modelInfoButton);
            modelsY += 12;
        }
    }

    private void addPageButtons() {
        addRenderableWidget(new FlatColorButton(x + 5, y + 28, 80, 18, Component.literal("<"), (b) -> {
            if (page > 0) {
                page--;
                this.init();
            }
        }));
        addRenderableWidget(new FlatColorButton(x + 260 - 85, y + 28, 80, 18, Component.literal(">"), (b) -> {
            if ((page + 1) * MAX_COUNT < this.modelsCount) {
                page++;
                this.init();
            }
        }));
    }

    private void addTopButtons() {
        FlatColorButton customButton = new FlatColorButton(x + 5, y + 5, 80, 18, Component.translatable("gui.yes_steve_model.model_manage.custom"), (b) -> {
            if (!isCustomModels) {
                isCustomModels = true;
                this.index = -1;
                page = 0;
                this.action = Action.EMPTY;
                this.init();
            }
        });
        customButton.setSelect(isCustomModels);
        addRenderableWidget(customButton);

        FlatColorButton authButton = new FlatColorButton(x + 90, y + 5, 80, 18, Component.translatable("gui.yes_steve_model.model_manage.auth"), (b) -> {
            if (isCustomModels) {
                isCustomModels = false;
                this.index = -1;
                page = 0;
                this.action = Action.EMPTY;
                this.init();
            }
        });
        authButton.setSelect(!isCustomModels);
        addRenderableWidget(authButton);

        addRenderableWidget(new FlatColorButton(x + 175, y + 5, 80, 18, Component.translatable("gui.yes_steve_model.model_manage.upload"), (b) -> {
            if (UploadManager.STATUE == UploadManager.Statue.FULFILL) {
                UploadManager.FILE_PATH = "";
            }
            this.action = Action.UPLOAD;
            this.init();
            if (UploadManager.STATUE == UploadManager.Statue.FULFILL) {
                new Thread(this::getUploadFilePath).start();
            }
        }));
    }

    private void getUploadFilePath() {
        String uploadFilePath = TinyFileDialogs.tinyfd_openFileDialog(I18n.get("gui.yes_steve_model.model_manage.open_file"), null, null, null, false);
        if (StringUtils.isBlank(uploadFilePath)) {
            return;
        }
        File file = Paths.get(uploadFilePath).toFile();
        if (FileUtils.isRegularFile(file)) {
            this.uploadError = null;
            if (!file.getName().endsWith("zip") && !file.getName().endsWith("ysm")) {
                this.uploadError = Component.translatable("gui.yes_steve_model.model_manage.error.format_incorrect");
                return;
            }
            if (FileUtils.sizeOf(file) > 32000) {
                this.uploadError = Component.translatable("gui.yes_steve_model.model_manage.error.too_large");
                return;
            }
            UploadManager.FILE_PATH = uploadFilePath;
            Minecraft.getInstance().submit(() -> this.init());
        }
    }

    private void uploadFile(String filePath, boolean isCustom) throws IOException {
        File file = Paths.get(filePath).toFile();
        if (FileUtils.isRegularFile(file)) {
            String name = file.getName();
            byte[] bytes = FileUtils.readFileToByteArray(file);
            UploadFile.Dir dir = isCustom ? UploadFile.Dir.CUSTOM : UploadFile.Dir.AUTH;
            NetworkHandler.CHANNEL.sendToServer(new UploadFile(name, bytes, dir));
            UploadManager.STATUE = UploadManager.Statue.PROCESSING;
        }
    }

    @Override
    @Keep
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(graphics);
        graphics.fillGradient(x, y, x + 260, y + 235, 0xff_222222, 0xff_222222);
        graphics.fillGradient(x + 265, y, x + 420, y + 235, 0xff_222222, 0xff_222222);
        graphics.fillGradient(x + 270, y + 5, x + 415, y + 23, 0xff_434242, 0xff_434242);

        graphics.drawCenteredString(font, Component.translatable("gui.yes_steve_model.model_manage.action_info"), x + 342, y + 11, 0xFFFFFF);
        graphics.drawString(font, String.format("%d/%d", page + 1, (this.modelsCount - 1) / MAX_COUNT + 1), x + 120, y + 33, 0xFFFFFF);

        if (this.action == Action.UPLOAD) {
            String folder = isCustomModels ? I18n.get("gui.yes_steve_model.model_manage.custom") : I18n.get("gui.yes_steve_model.model_manage.auth");
            String actionName = I18n.get("gui.yes_steve_model.model_manage." + this.action.name().toLowerCase(Locale.US));
            graphics.drawString(font, Component.translatable("gui.yes_steve_model.model_manage.selected", folder), x + 272, y + 29, 0xFFFFFF);
            graphics.drawString(font, Component.translatable("gui.yes_steve_model.model_manage.action", actionName), x + 272, y + 39, 0xFFFFFF);
            String uploadStatue = I18n.get("gui.yes_steve_model.model_manage.upload.statue." + UploadManager.STATUE.name().toLowerCase(Locale.US));
            graphics.drawString(font, Component.translatable("gui.yes_steve_model.model_manage.upload.statue", uploadStatue), x + 272, y + 49, 0xFFFFFF);
            String fileUpload = I18n.get("gui.yes_steve_model.model_manage.file.empty");
            if (StringUtils.isNoneBlank(UploadManager.FILE_PATH)) {
                fileUpload = UploadManager.FILE_PATH;
            }
            int yOffset = font.wordWrapHeight(fileUpload, 145);
            graphics.drawWordWrap(font, Component.translatable("gui.yes_steve_model.model_manage.file", fileUpload), x + 272, y + 59, 145, 0xFFFFFF);
            if (this.uploadError != null) {
                graphics.drawWordWrap(font, uploadError, x + 272, y + 60 + yOffset, 145, 0xFFFFFF);
            }
        }

        if (index >= 0 && index < getModels().size() && this.action != Action.UPLOAD) {
            RequestServerModelInfo.Info info = getModels().get(this.index);
            graphics.drawString(font, Component.translatable("gui.yes_steve_model.model_manage.selected", info.getFileName()), x + 272, y + 29, 0xFFFFFF);
            if (this.action != Action.EMPTY) {
                String actionName = I18n.get("gui.yes_steve_model.model_manage." + this.action.name().toLowerCase(Locale.US));
                graphics.drawString(font, Component.translatable("gui.yes_steve_model.model_manage.action", actionName), x + 272, y + 39, 0xFFFFFF);
            }
            if (this.action == Action.RENAME && textField != null) {
                textField.render(graphics, pMouseX, pMouseY, pPartialTick);
            }
        }
        super.render(graphics, pMouseX, pMouseY, pPartialTick);
    }

    private List<RequestServerModelInfo.Info> getModels() {
        if (isCustomModels) {
            return customModels;
        }
        return authModels;
    }

    @Override
    @Keep
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        if (textField != null) {
            String value = this.textField.getValue();
            super.resize(minecraft, width, height);
            this.textField.setValue(value);
        }
    }

    @Override
    @Keep
    public void tick() {
        if (textField != null) {
            this.textField.tick();
        }
    }

    public enum Action {
        DELETE, MOVE, RENAME, UPLOAD, EMPTY
    }
}
