package com.fox.ysmu.client.gui;

import com.fox.ysmu.ysmu;
import com.fox.ysmu.eep.ExtendedAuthModels;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.eep.ExtendedStarModels;
import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.client.gui.button.*;
import com.fox.ysmu.util.ModelIdUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class PlayerModelScreen extends Screen {
    protected final Player player;
    private Map<ResourceLocation, List<ResourceLocation>> models = Maps.newHashMap();
    private List<ResourceLocation> modelOrderList;
    private int maxPage;
    private EditBox textField;
    private Category category;
    private int page;
    private int x;
    private int y;

    public PlayerModelScreen() {
        super(Component.literal("YSM Player Model GUI"));
        this.category = Category.ALL;
        this.player = Minecraft.getInstance().player;
    }

    public PlayerModelScreen(Player player) {
        super(Component.literal("YSM Player Model GUI"));
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
                    if (eep.containModel(modelId) || !ClientModelManager.AUTH_MODELS.contains(modelId.getPath())) {
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
            String search = this.textField.getValue().toLowerCase(Locale.US);
            models.entrySet().removeIf(next -> !next.getKey().getPath().contains(search));
        }
        this.modelOrderList = Lists.newArrayList(models.keySet());
        this.modelOrderList.sort(ResourceLocation::compareTo);
        this.maxPage = (models.size() - 1) / 10;
    }

    @Override

    protected void init() {
        this.clearWidgets();
        this.calculateModelList();

        this.x = (width - 420) / 2;
        this.y = (height - 235) / 2;

        String perText = "";
        boolean focus = false;
        if (textField != null) {
            perText = textField.getValue();
            focus = textField.isFocused();
        }
        textField = new EditBox(getMinecraft().font, x + 144, y + 6, 140, 16, Component.literal("YSM Search Box"));
        textField.setValue(perText);
        textField.setTextColor(0xF3EFE0);
        textField.setFocused(focus);
        textField.moveCursorToEnd();
        this.addWidget(this.textField);

        addRenderableWidget(new TextureCountButton(x + 5, y + 5));
        addRenderableWidget(new FlatIconButton(x + 28, y + 5, 79, 20, 32, 16, (b) -> {
            ExtendedModelInfo eep = ExtendedModelInfo.get(player);
            if (eep != null) {
                List<ResourceLocation> textures = ClientModelManager.MODELS.get(eep.getModelId());
                if (textures != null) {
                    Minecraft.getInstance().setScreen(new PlayerTextureScreen(this, eep.getModelId(), textures));
                }
            }
        }).setTooltips("gui.yes_steve_model.model.texture"));
        addRenderableWidget(new StarButton(x + 110, y + 5));

        addRenderableWidget(new FlatIconButton(x + 328, y + 5, 18, 18, 32, 0, (b) -> {
            if (this.category != Category.ALL) {
                this.category = Category.ALL;
                this.page = 0;
                this.init();
            }
        }).setTooltips("gui.yes_steve_model.all_models"));
        addRenderableWidget(new FlatIconButton(x + 308, y + 5, 18, 18, 48, 0, (b) -> {
            if (this.category != Category.AUTH) {
                this.category = Category.AUTH;
                this.page = 0;
                this.init();
            }
        }).setTooltips("gui.yes_steve_model.auth_models"));
        addRenderableWidget(new FlatIconButton(x + 288, y + 5, 18, 18, 0, 0, (b) -> {
            if (this.category != Category.STAR) {
                this.category = Category.STAR;
                this.page = 0;
                this.init();
            }
        }).setTooltips("gui.yes_steve_model.star_models"));

        addRenderableWidget(new FlatIconButton(x + 397, y + 5, 18, 18, 16, 16, (b) -> {
            this.getMinecraft().setScreen(new ConfigScreen(this));
        }).setTooltips("gui.yes_steve_model.config"));
        addRenderableWidget(new FlatIconButton(x + 377, y + 5, 18, 18, 0, 16, (b) -> {
            this.getMinecraft().setScreen(new DownloadScreen(this));
        }).setTooltips("gui.yes_steve_model.download"));
        addRenderableWidget(new FlatIconButton(x + 357, y + 5, 18, 18, 80, 0, (b) -> {
            this.getMinecraft().setScreen(new OpenModelFolderScreen(this));
        }).setTooltips("gui.yes_steve_model.open_model_folder.open"));

        addRenderableWidget(new FlatColorButton(x + 198, y + 215, 52, 14, Component.translatable("gui.yes_steve_model.pre_page"), (b) -> {
            if (this.page > 0) {
                this.page--;
                this.init();
            }
        }));
        addRenderableWidget(new FlatColorButton(x + 308, y + 215, 52, 14, Component.translatable("gui.yes_steve_model.next_page"), (b) -> {
            if (this.page < this.maxPage) {
                this.page++;
                this.init();
            }
        }));

        if (this.page > this.maxPage) {
            this.page = 0;
        }

        for (int i = 0; i < 10; i++) {
            int modelIndex = i + this.page * 10;
            if (modelIndex >= models.size()) {
                break;
            }
            ResourceLocation id = modelOrderList.get(modelIndex);
            int xStart = x + 143 + 55 * (i % 5);
            int yStart = y + 28 + 93 * (i / 5);
            ExtendedAuthModels eep = ExtendedAuthModels.get(player)
            if (eep != null) {
                if (ClientModelManager.AUTH_MODELS.contains(id.getPath()) && !eep.containModel(id)) {
                    addRenderableWidget(new ModelButton(xStart, yStart, true, Pair.of(id, models.get(id)), ClientModelManager.EXTRA_INFO.get(ModelIdUtil.getMainId(id)), player));
                } else {
                    addRenderableWidget(new ModelButton(xStart, yStart, false, Pair.of(id, models.get(id)), ClientModelManager.EXTRA_INFO.get(ModelIdUtil.getMainId(id)), player));
                }
            }
        }
    }

    @Override

    @SuppressWarnings("all")
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);

        graphics.fillGradient(x, y, x + 135, y + 235, 0xff_222222, 0xff_222222);
        graphics.fillGradient(x + 138, y, x + 420, y + 235, 0xff_222222, 0xff_222222);
        graphics.fillGradient(x + 351, y + 7, x + 352, y + 21, 0xFF_F3EFE0, 0xFF_F3EFE0);

        textField.render(graphics, mouseX, mouseY, partialTicks);

        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int scissorX = (int) ((this.x + 5) * scale);
        int scissorY = (int) (window.getHeight() - ((this.y + 200) * scale));
        int scissorW = (int) (125 * scale);
        int scissorH = (int) (171 * scale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 67, y + 190, 70, x + 67 - mouseX, y + 180 - 95 - mouseY, player);
        RenderSystem.disableScissor();

        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep != null) {
            String modelName = eep.getModelId().getPath();
            List<FormattedCharSequence> modelNameSplit = font.split(FormattedText.of(modelName), 125);
            int lineY = y + 205;
            for (FormattedCharSequence line : modelNameSplit) {
                int nameWidth = font.width(line);
                graphics.drawString(font, line, x + (135 - nameWidth) / 2, lineY, 0xF3EFE0);
                lineY += 10;
            }
        }

        if (textField.getValue().isEmpty() && !textField.isFocused()) {
            graphics.drawString(font, Component.translatable("gui.yes_steve_model.search").withStyle(ChatFormatting.ITALIC), x + 148, y + 10, 0x777777);
        }

        String pageInfo = String.format("%d/%d", page + 1, this.maxPage + 1);
        graphics.drawString(font, pageInfo, x + 138 + (282 - font.width(pageInfo)) / 2, y + 223 - font.lineHeight / 2, 0xF3EFE0);

        String debugInfo = String.format("%s-%s", SharedConstants.getCurrentVersion().getName(), ModList.get().getModFileById(ysmu.MODID).versionString());
        graphics.drawString(font, debugInfo, x + 2, y + 226, ChatFormatting.DARK_GRAY.getColor());

        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderables.stream().filter(r -> r instanceof FlatIconButton).forEach(r -> ((FlatIconButton) r).renderToolTip(graphics, this, mouseX, mouseY));
        this.renderables.stream().filter(r -> r instanceof ModelButton).forEach(r -> ((ModelButton) r).renderComponentTooltip(graphics, this, mouseX, mouseY));
    }

    @Override

    public void resize(Minecraft minecraft, int width, int height) {
        String value = this.textField.getValue();
        super.resize(minecraft, width, height);
        this.textField.setValue(value);
    }

    @Override

    public void tick() {
        this.textField.tick();
    }

    @Override

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.textField.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.textField);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override

    public boolean charTyped(char codePoint, int modifiers) {
        if (textField == null) {
            return false;
        }
        String perText = this.textField.getValue();
        if (this.textField.charTyped(codePoint, modifiers)) {
            if (!Objects.equals(perText, this.textField.getValue())) {
                this.page = 0;
                this.init();
            }
            return true;
        }
        return false;
    }

    @Override

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean hasKeyCode = InputConstants.getKey(keyCode, scanCode).getNumericKeyValue().isPresent();
        String preText = this.textField.getValue();
        if (hasKeyCode) {
            return true;
        }
        if (this.textField.keyPressed(keyCode, scanCode, modifiers)) {
            if (!Objects.equals(preText, this.textField.getValue())) {
                this.page = 0;
                this.init();
            }
            return true;
        } else {
            return this.textField.isFocused() && this.textField.isVisible() && keyCode != 256 || super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override

    protected void insertText(String text, boolean overwrite) {
        if (overwrite) {
            this.textField.setValue(text);
        } else {
            this.textField.insertText(text);
        }
    }

    @Override

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (minecraft == null) {
            return false;
        }
        if (delta != 0 && inRange(mouseX, mouseY)) {
            return scrollPage(delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean inRange(double mouseX, double mouseY) {
        boolean isInWidthRange = (x + 143) < mouseX && mouseX < (x + 430);
        boolean isInHeightRange = (y + 25) < mouseY && mouseY < (y + 235);
        return isInWidthRange && isInHeightRange;
    }

    private boolean scrollPage(double delta) {
        if (delta > 0 && this.page > 0) {
            this.page--;
            getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.init();
        }
        if (delta < 0 && this.page < this.maxPage) {
            this.page++;
            getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.init();
        }
        return true;
    }

    @Override

    public boolean isPauseScreen() {
        return false;
    }

    private enum Category {
        /**
         * 不同页面类别
         */
        ALL, AUTH, STAR
    }
}
