package com.fox.ysmu.client.gui;

import com.fox.ysmu.Tags;
import com.fox.ysmu.compat.YsmConverter;
import com.fox.ysmu.eep.ExtendedAuthModels;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.eep.ExtendedStarModels;
import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.client.gui.button.*;
import com.fox.ysmu.util.ModelIdUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.*;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.fox.ysmu.model.ServerModelManager.CUSTOM;
import static com.fox.ysmu.model.ServerModelManager.removeExtension;

public class PlayerModelScreen extends GuiScreen {
    protected final EntityPlayer player;
    private Map<ResourceLocation, List<ResourceLocation>> models = Maps.newHashMap();
    private List<ResourceLocation> modelOrderList;
    private int maxPage;
    private GuiTextField textField;
    private Category category;
    private int page;
    private int x;
    private int y;

    public PlayerModelScreen() {
        this.category = Category.ALL;
        this.player = Minecraft.getMinecraft().thePlayer;
    }

    public PlayerModelScreen(EntityPlayer player) {
        this.category = Category.ALL;
        this.player = player;
    }

    private void calculateModelList() {
        models = Maps.newHashMap();
        if (this.category == Category.ALL) {
            this.models.putAll(ClientModelManager.MODELS);
        }
        if (this.category == Category.AUTH) {
            ExtendedAuthModels eep = ExtendedAuthModels.get(this.player);
            if (eep != null) {
                for (ResourceLocation modelId : ClientModelManager.MODELS.keySet()) {
                    if (eep.containModel(modelId) || !ClientModelManager.AUTH_MODELS.contains(modelId.getResourcePath())) {
                        this.models.put(modelId, ClientModelManager.MODELS.get(modelId));
                    }
                }
            }
        }
        if (this.category == Category.STAR) {
            ExtendedStarModels eep = ExtendedStarModels.get(this.player);
            if (eep != null) {
                for (ResourceLocation modelId : ClientModelManager.MODELS.keySet()) {
                    if (eep.containModel(modelId)) {
                        this.models.put(modelId, ClientModelManager.MODELS.get(modelId));
                    }
                }
            }
        }
        if (textField != null) {
            String search = this.textField.getText().toLowerCase(Locale.US);
            models.entrySet().removeIf(next -> !next.getKey().getResourcePath().contains(search));
        }
        this.modelOrderList = Lists.newArrayList(models.keySet());
        this.modelOrderList.sort(Comparator.comparing(ResourceLocation::toString));
        this.maxPage = (models.size() - 1) / 10;
    }

    @Override
    public void initGui() {
        // clearWidgets() -> buttonList.clear()
        this.buttonList.clear();
        this.calculateModelList();

        this.x = (width - 420) / 2;
        this.y = (height - 235) / 2;

        String perText = "";
        boolean focus = false;
        if (textField != null) {
            perText = textField.getText();
            focus = textField.isFocused();
        }
        textField = new GuiTextField(this.fontRendererObj, x + 144, y + 6, 140, 16);
        textField.setText(perText);
        textField.setTextColor(0xF3EFE0);
        textField.setFocused(focus);
        textField.setCursorPositionEnd();

        // 按钮创建和点击逻辑分离 使用唯一的 ID 来标识按钮
        // addRenderableWidget -> this.buttonList.add
        this.buttonList.add(new TextureCountButton(0, x + 5, y + 5));
        this.buttonList.add(new FlatIconButton(1, x + 28, y + 5, 79, 20, 32, 16).setTooltips("gui.yes_steve_model.model.texture"));
        this.buttonList.add(new StarButton(2, x + 110, y + 5));
        this.buttonList.add(new FlatIconButton(3, x + 328, y + 5, 18, 18, 32, 0).setTooltips("gui.yes_steve_model.all_models"));
        this.buttonList.add(new FlatIconButton(4, x + 308, y + 5, 18, 18, 48, 0).setTooltips("gui.yes_steve_model.auth_models"));
        this.buttonList.add(new FlatIconButton(5, x + 288, y + 5, 18, 18, 0, 0).setTooltips("gui.yes_steve_model.star_models"));
        this.buttonList.add(new FlatIconButton(6, x + 397, y + 5, 18, 18, 16, 16).setTooltips("gui.yes_steve_model.config"));
        this.buttonList.add(new FlatIconButton(7, x + 377, y + 5, 18, 18, 48, 16).setTooltips("gui.yes_steve_model.fix"));
        this.buttonList.add(new FlatIconButton(8, x + 357, y + 5, 18, 18, 80, 0).setTooltips("gui.yes_steve_model.open_model_folder.open"));
        this.buttonList.add(new FlatColorButton(9, x + 198, y + 215, 52, 14, I18n.format("gui.yes_steve_model.pre_page")));
        this.buttonList.add(new FlatColorButton(10, x + 308, y + 215, 52, 14, I18n.format("gui.yes_steve_model.next_page")));

        if (this.page > this.maxPage) {
            this.page = 0;
        }
        int buttonId = 11;
        for (int i = 0; i < 10; i++) {
            int modelIndex = i + this.page * 10;
            if (modelIndex >= models.size()) {
                break;
            }
            ResourceLocation id = modelOrderList.get(modelIndex);
            int xStart = x + 143 + 55 * (i % 5);
            int yStart = y + 28 + 93 * (i / 5);
            ExtendedAuthModels eep = ExtendedAuthModels.get(player);
            if (eep != null) {
                boolean needAuth = false;
                this.buttonList.add(new ModelButton(buttonId++, xStart, yStart, needAuth, Pair.of(id, models.get(id)), ClientModelManager.EXTRA_INFO.get(ModelIdUtil.getMainId(id)), player));
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                break;
            case 1:
                ExtendedModelInfo eep = ExtendedModelInfo.get(player);
                if (eep != null) {
                    List<ResourceLocation> textures = ClientModelManager.MODELS.get(eep.getModelId());
                    if (textures != null) {
                        // setScreen -> displayGuiScreen
                        this.mc.displayGuiScreen(new PlayerTextureScreen(this, eep.getModelId(), textures));
                    }
                }
                break;
            case 2:
                if (button instanceof StarButton) {
                    ((StarButton) button).doPress();
                }
                break;
            case 3:
                if (this.category != Category.ALL) {
                    this.category = Category.ALL;
                    this.page = 0;
                    this.initGui();
                }
                break;
            case 4:
                if (this.category != Category.AUTH) {
                    this.category = Category.AUTH;
                    this.page = 0;
                    this.initGui();
                }
                break;
            case 5:
                if (this.category != Category.STAR) {
                    this.category = Category.STAR;
                    this.page = 0;
                    this.initGui();
                }
                break;
            case 6:
                this.mc.displayGuiScreen(new ConfigScreen(this));
                break;
            case 7:
                fix();
                break;
            case 8:
                this.mc.displayGuiScreen(new OpenModelFolderScreen(this));
                break;
            case 9:
                if (this.page > 0) {
                    this.page--;
                    this.initGui();
                }
                break;
            case 10:
                if (this.page < this.maxPage) {
                    this.page++;
                    this.initGui();
                }
                break;
            default:
                if (button instanceof ModelButton) {
                    ((ModelButton) button).doPress();
                }
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // renderBackground(graphics) -> drawDefaultBackground()
        this.drawDefaultBackground();

        this.drawGradientRect(x, y, x + 135, y + 235, 0xff_222222, 0xff_222222);
        this.drawGradientRect(x + 138, y, x + 420, y + 235, 0xff_222222, 0xff_222222);
        this.drawGradientRect(x + 351, y + 7, x + 352, y + 21, 0xFF_F3EFE0, 0xFF_F3EFE0);
        // textField.render -> textField.drawTextBox
        textField.drawTextBox();

        int scale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        int scissorX = (this.x + 5) * scale;
        int scissorY = mc.displayHeight - ((this.y + 200) * scale);
        int scissorW = 125 * scale;
        int scissorH = 171 * scale;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
        // func_147046_a(x,y,scale,toMouseX,toMouseY,entity)
        GuiInventory.func_147046_a(x + 67, y + 190, 70, x + 67 - mouseX, y + 180 - 95 - mouseY, player);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep != null) {
            String modelName = eep.getModelId().getResourcePath();
            // font -> fontRendererObj
            List<String> modelNameSplit = fontRendererObj.listFormattedStringToWidth(modelName, 125);
            int lineY = y + 205;
            for (String line : modelNameSplit) {
                int nameWidth = fontRendererObj.getStringWidth(line);
                this.drawString(fontRendererObj, line, x + (135 - nameWidth) / 2, lineY, 0xF3EFE0);
                lineY += 10;
            }
        }

        if (textField.getText().isEmpty() && !textField.isFocused()) {
            this.drawString(fontRendererObj, EnumChatFormatting.ITALIC + I18n.format("gui.yes_steve_model.search"), x + 148, y + 10, 0x777777);
        }

        String pageInfo = String.format("%d/%d", page + 1, this.maxPage + 1);
        this.drawString(fontRendererObj, pageInfo, x + 138 + (282 - fontRendererObj.getStringWidth(pageInfo)) / 2, y + 223 - fontRendererObj.FONT_HEIGHT / 2, 0xF3EFE0);

        String debugInfo = String.format("%s-%s", "1.7.10", Tags.VERSION);
        this.drawString(fontRendererObj, debugInfo, x + 2, y + 226, 0x555555);
        // super.render -> super.drawScreen, 这会绘制所有按钮
        super.drawScreen(mouseX, mouseY, partialTicks);
        // Render tooltips
        for (Object button : this.buttonList) {
            if (button instanceof FlatIconButton f) {
                if (f.func_146115_a() && f.tooltips != null && !f.tooltips.isEmpty()) {
                    this.func_146283_a(f.tooltips, mouseX, mouseY);
                }
            }
            if (button instanceof ModelButton m) {
                if (m.func_146115_a() && m.tooltips != null && !m.tooltips.isEmpty()) {
                    List<String> tooltipStrings = m.tooltips.stream().map(IChatComponent::getFormattedText).collect(Collectors.toList());
                    this.func_146283_a(tooltipStrings, mouseX, mouseY);
                }
            }
        }
    }

    // tick -> updateScreen
    @Override
    public void updateScreen() {
        this.textField.updateCursorCounter();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        this.textField.mouseClicked(mouseX, mouseY, button);
    }

    // charTyped and keyPressed -> keyTyped
    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        String perText = this.textField.getText();
        if (this.textField.textboxKeyTyped(typedChar, keyCode)) {
            if (!Objects.equals(perText, this.textField.getText())) {
                this.page = 0;
                this.initGui();
            }
        } else {
            if (this.textField.isFocused() && keyCode != 1) {
                return; // 阻止其他按键（如E键）关闭GUI
            }
            super.keyTyped(typedChar, keyCode);
        }
    }

    // mouseScrolled -> handleMouseInput
    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            if (inRange(mouseX, mouseY)) {
                scrollPage(dWheel);
            }
        }
    }

    private boolean inRange(int mouseX, int mouseY) {
        boolean isInWidthRange = (x + 143) < mouseX && mouseX < (x + 430);
        boolean isInHeightRange = (y + 25) < mouseY && mouseY < (y + 235);
        return isInWidthRange && isInHeightRange;
    }

    private void scrollPage(int delta) {
        if (delta > 0 && this.page > 0) {
            this.page--;
            this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
            this.initGui();
        }
        if (delta < 0 && this.page < this.maxPage) {
            this.page++;
            this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
            this.initGui();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private enum Category {
        /**
         * 不同页面类别
         */
        ALL, AUTH, STAR
    }

    private void fix() {
        File customDir = CUSTOM.toFile();
        if (!customDir.exists() || !customDir.isDirectory()) return;
        EntityPlayer player = this.mc.thePlayer;
        boolean isFileChanged = false;

        File[] ysmFiles = customDir.listFiles((dir, name) -> name.endsWith(".ysm"));
        if (ysmFiles != null) {
            for (File file : ysmFiles) {
                String rawName = removeExtension(file.getName());
                String validName = YsmConverter.sanitizeDirName(rawName);
                if (!rawName.equals(validName)) {
                    File newFile = new File(customDir, validName + ".ysm");
                    if (!newFile.exists()) {
                        file.renameTo(newFile);
                        isFileChanged = true;
                        player.addChatMessage(new ChatComponentTranslation(
                            "message.yes_steve_model.model.compat.rename", rawName ,validName));
                    }
                }
            }
        }

        Set<String> loadedModelNames = new HashSet<>();
        if (modelOrderList != null) {
            for (ResourceLocation resourceLocation : modelOrderList) {
                loadedModelNames.add(resourceLocation.getResourcePath());
            }
        }
        File[] subDirs = customDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File folder : subDirs) {
                String folderName = folder.getName();
                if (!loadedModelNames.contains(folderName)) {
                    try {
                        YsmConverter.convertAndReplace(folder, customDir);
                        isFileChanged = true;
                        String newName = YsmConverter.sanitizeDirName(folderName);
                        player.addChatMessage(new ChatComponentTranslation(
                            "message.yes_steve_model.model.compat.fix.true", folderName, newName));
                    } catch (Exception e) {
                        e.printStackTrace();
                        player.addChatMessage(new ChatComponentTranslation(
                            "message.yes_steve_model.model.compat.fix.false", folderName));
                    }
                }
            }
        }

        if (isFileChanged) {
            IChatComponent reload = new ChatComponentTranslation("message.yes_steve_model.model.compat.click");
            reload.getChatStyle().setColor(EnumChatFormatting.GOLD);
            reload.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ysm reload"));
            reload.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("/ysm reload")));
            player.addChatMessage(new ChatComponentTranslation("message.yes_steve_model.model.compat.reload", reload));
        }
    }
}
