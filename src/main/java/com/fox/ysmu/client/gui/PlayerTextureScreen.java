package com.fox.ysmu.client.gui;

import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.client.gui.button.FlatColorButton;
import com.fox.ysmu.client.gui.button.FlatIconButton;
import com.fox.ysmu.client.gui.button.TextureButton;
import com.fox.ysmu.util.RenderUtil;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class PlayerTextureScreen extends Screen {
    private static final float SCALE_MAX = 360f;
    private static final float SCALE_MIN = 18f;
    private static final float PITCH_MAX = 90f;
    private static final float PITCH_MIN = -90f;

    private static final int LEFT_MOUSE_BUTTON = 0;
    private static final int RIGHT_MOUSE_BUTTON = 1;

    private final PlayerModelScreen parent;
    private final ResourceLocation modelId;
    private final List<ResourceLocation> textures;
    private final List<String> animations;
    private final Player player;
    private String animation = "";
    private int maxTexturePage;
    private int texturePage;
    private int maxAnimationPage;
    private int animationPage;
    private int x;
    private int y;

    private float posX = 0;
    private float posY = -60;
    private float scale = 80;
    private float yaw = 165;
    private float pitch = -5;
    private boolean showGround = true;


    public PlayerTextureScreen(PlayerModelScreen parent, ResourceLocation modelId, List<ResourceLocation> textures) {
        super(Component.literal("Player Texture GUI"));
        this.parent = parent;
        this.modelId = modelId;
        this.textures = textures;
        this.textures.sort(ResourceLocation::compareTo);
        this.animations = new ArrayList<>(ClientModelManager.DEFAULT_ANIMATION_FILE.animations().keySet().stream().toList());
        this.animations.sort(String::compareTo);
        this.player = parent.player;
    }

    @Override

    protected void init() {
        this.clearWidgets();

        this.x = (width - 420) / 2;
        this.y = (height - 235) / 2;
        this.maxTexturePage = (textures.size() - 1) / 4;
        this.maxAnimationPage = (animations.size() - 1) / 11;
        if (this.texturePage > this.maxTexturePage) {
            this.texturePage = 0;
        }
        if (this.animationPage > this.maxAnimationPage) {
            this.animationPage = 0;
        }

        addRenderableWidget(new FlatColorButton(x + 5, y, 80, 18, Component.translatable("gui.yes_steve_model.model.return"), (b) -> this.getMinecraft().setScreen(parent)));

        addRenderableWidget(new FlatIconButton(x + 281, y + 2, 16, 16, 64, 16, (b) -> {
            this.animation = "";
        }).setTooltips("gui.yes_steve_model.model.stop"));
        addRenderableWidget(new FlatIconButton(x + 263, y + 2, 16, 16, 48, 16, (b) -> {
            this.posX = 0;
            this.posY = -60;
            this.scale = 80;
            this.yaw = 165;
            this.pitch = -5;
        }).setTooltips("gui.yes_steve_model.model.reset"));
        addRenderableWidget(new FlatIconButton(x + 245, y + 2, 16, 16, 64, 0, (b) -> {
            this.showGround = !this.showGround;
        }).setTooltips("gui.yes_steve_model.model.ground"));

        addRenderableWidget(new FlatColorButton(x + 321, y + 213, 18, 18, Component.literal("<"), (b) -> {
            if (this.texturePage > 0) {
                this.texturePage--;
                this.init();
            }
        }));
        addRenderableWidget(new FlatColorButton(x + 383, y + 213, 18, 18, Component.literal(">"), (b) -> {
            if (this.texturePage < this.maxTexturePage) {
                this.texturePage++;
                this.init();
            }
        }));
        addRenderableWidget(new FlatColorButton(x + 11, y + 214, 16, 16, Component.literal("<"), (b) -> {
            if (this.animationPage > 0) {
                this.animationPage--;
                this.init();
            }
        }));
        addRenderableWidget(new FlatColorButton(x + 63, y + 214, 16, 16, Component.literal(">"), (b) -> {
            if (this.animationPage < this.maxAnimationPage) {
                this.animationPage++;
                this.init();
            }
        }));


        for (int i = 0; i < 11; i++) {
            int animationIndex = i + this.animationPage * 11;
            if (animationIndex >= animations.size()) {
                break;
            }
            String name = animations.get(animationIndex);
            int yStart = y + 27 + 17 * i;
            String key = String.format("gui.yes_steve_model.texture.button.%s", name.replaceAll("\\:", "."));
            String keyDesc = String.format("gui.yes_steve_model.texture.button.%s.desc", name.replaceAll("\\:", "."));
            FlatColorButton sideButton = new FlatColorButton(x + 5, yStart, 80, 16, Component.translatable(key), b -> this.animation = name);
            sideButton.setTooltips(Lists.newArrayList(Component.translatable(keyDesc).withStyle(ChatFormatting.GOLD),
                    Component.translatable("gui.yes_steve_model.texture.button.animation_name", name).withStyle(ChatFormatting.GRAY)));
            addRenderableWidget(sideButton);
        }

        for (int i = 0; i < 4; i++) {
            int modelIndex = i + this.texturePage * 4;
            if (modelIndex >= textures.size()) {
                break;
            }
            int xStart = x + 306 + 56 * (i % 2);
            int yStart = y + 5 + 104 * (i / 2);
            addRenderableWidget(new TextureButton(xStart, yStart, modelId, textures.get(modelIndex), player));
        }
    }

    @Override

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fillGradient(x, y + 22, x + 90, y + 235, 0xff_222222, 0xff_222222);
        graphics.fillGradient(x + 93, y, x + 299, y + 235, 0xff_222222, 0xff_222222);
        graphics.fillGradient(x + 302, y, x + 420, y + 235, 0xff_222222, 0xff_222222);

        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep != null) {
            Window window = Minecraft.getInstance().getWindow();
            double guiScale = window.getGuiScale();
            int scissorX = (int) ((this.x + 93) * guiScale);
            int scissorY = (int) (window.getHeight() - ((this.y + 235) * guiScale));
            int scissorW = (int) (206 * guiScale);
            int scissorH = (int) (235 * guiScale);
            RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
            RenderUtil.renderTextureScreenEntity(this.x + 299 / 2.0F + 40 + posX, this.y + 235 / 2.0F + 80 + posY, scale, pitch, yaw, player, modelId, eep.getSelectTexture(), showGround, entity -> {
                if (!entity.hasPreviewAnimation(animation)) {
                    entity.setPreviewAnimation(animation);
                }
            });
            RenderSystem.disableScissor();
        }

        String texturePageInfo = String.format("%d/%d", texturePage + 1, this.maxTexturePage + 1);
        graphics.drawString(font, texturePageInfo, x + 302 + (118 - font.width(texturePageInfo)) / 2, y + 223 - font.lineHeight / 2, 0xF3EFE0);

        String animationPageInfo = String.format("%d/%d", animationPage + 1, this.maxAnimationPage + 1);
        graphics.drawString(font, animationPageInfo, x + 5 + (80 - font.width(animationPageInfo)) / 2, y + 218, 0xF3EFE0);

        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderables.stream().filter(r -> r instanceof FlatColorButton)
                .forEach(r -> ((FlatColorButton) r).renderToolTip(graphics, this, mouseX, mouseY));
    }

    @Override

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (minecraft == null || !inViewRange(mouseX, mouseY)) {
            return false;
        }
        if (button == LEFT_MOUSE_BUTTON) {
            yaw += (1.5 * dragX);
            changePitchValue((float) dragY);
        }
        if (button == RIGHT_MOUSE_BUTTON) {
            posX += dragX;
            posY += dragY;
        }
        return true;
    }

    @Override

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (minecraft == null) {
            return false;
        }
        if (delta != 0) {
            if (inViewRange(mouseX, mouseY)) {
                changeScaleValue((float) delta * 0.07f);
                return true;
            }
            if (inAnimationRange(mouseX, mouseY)) {
                return scrollAnimationPage(delta);
            }
            if (inTextureRange(mouseX, mouseY)) {
                return scrollTexturePage(delta);
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean scrollTexturePage(double delta) {
        if (delta > 0 && this.texturePage > 0) {
            this.texturePage--;
            getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.init();
        }
        if (delta < 0 && this.texturePage < this.maxTexturePage) {
            this.texturePage++;
            getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.init();
        }
        return true;
    }

    private boolean scrollAnimationPage(double delta) {
        if (delta > 0 && this.animationPage > 0) {
            this.animationPage--;
            getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.init();
        }
        if (delta < 0 && this.animationPage < this.maxAnimationPage) {
            this.animationPage++;
            getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.init();
        }
        return true;
    }

    private boolean inViewRange(double mouseX, double mouseY) {
        boolean isInWidthRange = (x + 93) < mouseX && mouseX < (x + 299);
        boolean isInHeightRange = y < mouseY && mouseY < (y + 235);
        return isInWidthRange && isInHeightRange;
    }

    private boolean inAnimationRange(double mouseX, double mouseY) {
        boolean isInWidthRange = x < mouseX && mouseX < (x + 90);
        boolean isInHeightRange = (y + 22) < mouseY && mouseY < (y + 235);
        return isInWidthRange && isInHeightRange;
    }

    private boolean inTextureRange(double mouseX, double mouseY) {
        boolean isInWidthRange = (x + 302) < mouseX && mouseX < (x + 420);
        boolean isInHeightRange = y < mouseY && mouseY < (y + 235);
        return isInWidthRange && isInHeightRange;
    }

    private void changePitchValue(float amount) {
        if (pitch - amount > PITCH_MAX) {
            pitch = 90;
        } else if (pitch - amount < PITCH_MIN) {
            pitch = -90;
        } else {
            pitch = pitch - amount;
        }
    }

    private void changeScaleValue(float amount) {
        float tmp = scale + amount * scale;
        scale = Mth.clamp(tmp, SCALE_MIN, SCALE_MAX);
    }

    @Override

    public boolean isPauseScreen() {
        return false;
    }
}
