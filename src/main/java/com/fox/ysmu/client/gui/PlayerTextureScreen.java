package com.fox.ysmu.client.gui;

import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.client.gui.button.FlatColorButton;
import com.fox.ysmu.client.gui.button.FlatIconButton;
import com.fox.ysmu.client.gui.button.TextureButton;
import com.fox.ysmu.util.RenderUtil;
import com.google.common.collect.Lists;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerTextureScreen extends GuiScreen {
    private static final float SCALE_MAX = 360f;
    private static final float SCALE_MIN = 18f;
    private static final float PITCH_MAX = 90f;
    private static final float PITCH_MIN = -90f;

    private static final int LEFT_MOUSE_BUTTON = 0;
    private static final int RIGHT_MOUSE_BUTTON = 1;
    private int lastMouseX;
    private int lastMouseY;

    private final PlayerModelScreen parent;
    private final ResourceLocation modelId;
    private final List<ResourceLocation> textures;
    private final List<String> animations;
    private final EntityPlayer player;
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
        this.parent = parent;
        this.modelId = modelId;
        this.textures = textures;
        this.textures.sort(Comparator.comparing(ResourceLocation::toString));
        this.animations = new ArrayList<>(ClientModelManager.DEFAULT_ANIMATION_FILE.animations.keySet().stream().collect(Collectors.toList()));
        this.animations.sort(String::compareTo);
        this.player = parent.player;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
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
        this.buttonList.add(new FlatColorButton(0, x + 5, y, 80, 18, I18n.format("gui.yes_steve_model.model.return")));
        this.buttonList.add(new FlatIconButton(1, x + 281, y + 2, 16, 16, 64, 16).setTooltips("gui.yes_steve_model.model.stop"));
        this.buttonList.add(new FlatIconButton(2, x + 263, y + 2, 16, 16, 48, 16).setTooltips("gui.yes_steve_model.model.reset"));
        this.buttonList.add(new FlatIconButton(3, x + 245, y + 2, 16, 16, 64, 0).setTooltips("gui.yes_steve_model.model.ground"));
        this.buttonList.add(new FlatColorButton(4, x + 321, y + 213, 18, 18, "<"));
        this.buttonList.add(new FlatColorButton(5, x + 383, y + 213, 18, 18, ">"));
        this.buttonList.add(new FlatColorButton(6, x + 11, y + 214, 16, 16, "<"));
        this.buttonList.add(new FlatColorButton(7, x + 63, y + 214, 16, 16, ">"));

        int a = 10;
        for (int i = 0; i < 11; i++) {
            int animationIndex = i + this.animationPage * 11;
            if (animationIndex >= animations.size()) {
                break;
            }
            String name = animations.get(animationIndex);
            int yStart = y + 27 + 17 * i;
            String key = String.format("gui.yes_steve_model.texture.button.%s", name.replaceAll("\\:", "."));
            String keyDesc = String.format("gui.yes_steve_model.texture.button.%s.desc", name.replaceAll("\\:", "."));
            FlatColorButton sideButton = new FlatColorButton(a++, x + 5, yStart, 80, 16, I18n.format(key));
            sideButton.setTooltips(Lists.newArrayList(
                EnumChatFormatting.GOLD + I18n.format(keyDesc),
                EnumChatFormatting.GRAY + I18n.format("gui.yes_steve_model.texture.button.animation_name", name)
            ));
            this.buttonList.add(sideButton);
        }

        int b = 30;
        for (int i = 0; i < 4; i++) {
            int modelIndex = i + this.texturePage * 4;
            if (modelIndex >= textures.size()) {
                break;
            }
            int xStart = x + 306 + 56 * (i % 2);
            int yStart = y + 5 + 104 * (i / 2);
            this.buttonList.add(new TextureButton(b++, xStart, yStart, modelId, textures.get(modelIndex), player));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                this.mc.displayGuiScreen(parent);
                break;
            case 1:
                this.animation = "";
                break;
            case 2:
                this.posX = 0;
                this.posY = -60;
                this.scale = 80;
                this.yaw = 165;
                this.pitch = -5;
                break;
            case 3:
                this.showGround = !this.showGround;
                break;
            case 4:
                if (this.texturePage > 0) {
                    this.texturePage--;
                    this.initGui();
                }
                break;
            case 5:
                if (this.texturePage < this.maxTexturePage) {
                    this.texturePage++;
                    this.initGui();
                }
                break;
            case 6:
                if (this.animationPage > 0) {
                    this.animationPage--;
                    this.initGui();
                }
                break;
            case 7:
                if (this.animationPage < this.maxAnimationPage) {
                    this.animationPage++;
                    this.initGui();
                }
                break;
            default:
                if (button.id >= 10 && button.id < 30) {
                    int animationIndex = (button.id - 10) + this.animationPage * 11;
                    if (animationIndex < this.animations.size()) {
                        this.animation = this.animations.get(animationIndex);
                    }
                }
                if (button instanceof TextureButton) {
                    ((TextureButton) button).doPress();
                }
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        this.drawDefaultBackground();
        this.drawGradientRect(x, y + 22, x + 90, y + 235, 0xff_222222, 0xff_222222);
        this.drawGradientRect(x + 93, y, x + 299, y + 235, 0xff_222222, 0xff_222222);
        this.drawGradientRect(x + 302, y, x + 420, y + 235, 0xff_222222, 0xff_222222);

        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep != null) {
            int guiScale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
            int scissorX = (this.x + 93) * guiScale;
            int scissorY = mc.displayHeight - ((this.y + 235) * guiScale);
            int scissorW = 206 * guiScale;
            int scissorH = 235 * guiScale;
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
            RenderUtil.renderTextureScreenEntity(this.x + 299 / 2.0F + 40 + posX, this.y + 235 / 2.0F + 80 + posY, scale, pitch, yaw, player, modelId, eep.getSelectTexture(), showGround, entity -> {
                if (!entity.hasPreviewAnimation(animation)) {
                    entity.setPreviewAnimation(animation);
                }
            });
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        String texturePageInfo = String.format("%d/%d", texturePage + 1, this.maxTexturePage + 1);
        this.drawString(fontRendererObj, texturePageInfo, x + 302 + (118 - fontRendererObj.getStringWidth(texturePageInfo)) / 2, y + 223 - fontRendererObj.FONT_HEIGHT / 2, 0xF3EFE0);

        String animationPageInfo = String.format("%d/%d", animationPage + 1, this.maxAnimationPage + 1);
        this.drawString(fontRendererObj, animationPageInfo, x + 5 + (80 - fontRendererObj.getStringWidth(animationPageInfo)) / 2, y + 218, 0xF3EFE0);

        super.drawScreen(mouseX, mouseY, partialTick);
        for (Object button : this.buttonList) {
            if (button instanceof FlatColorButton fc) {
                if (fc.func_146115_a() && fc.tooltips != null && !fc.tooltips.isEmpty()) {
                    this.func_146283_a(fc.tooltips, mouseX, mouseY);
                }
            }
            if (button instanceof FlatIconButton fi) {
                if (fi.func_146115_a() && fi.tooltips != null && !fi.tooltips.isEmpty()) {
                    this.func_146283_a(fi.tooltips, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (inViewRange(mouseX, mouseY) && (mouseButton == LEFT_MOUSE_BUTTON || mouseButton == RIGHT_MOUSE_BUTTON)) {
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (mc == null || !inViewRange(mouseX, mouseY)) {
            return;
        }
        float dragX = mouseX - this.lastMouseX;
        float dragY = mouseY - this.lastMouseY;
        if (button == LEFT_MOUSE_BUTTON) {
            yaw += (1.5 * dragX);
            changePitchValue(dragY);
        }
        if (button == RIGHT_MOUSE_BUTTON) {
            posX += dragX;
            posY += dragY;
        }
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            if (inViewRange(mouseX, mouseY)) {
                changeScaleValue((float) dWheel * 0.07f);
            }
            if (inAnimationRange(mouseX, mouseY)) {
                scrollAnimationPage(dWheel);
            }
            if (inTextureRange(mouseX, mouseY)) {
                scrollTexturePage(dWheel);
            }
        }
    }

    private void scrollTexturePage(int delta) {
        if (delta > 0 && this.texturePage > 0) {
            this.texturePage--;
            this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
            this.initGui();
        }
        if (delta < 0 && this.texturePage < this.maxTexturePage) {
            this.texturePage++;
            this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
            this.initGui();
        }
    }

    private void scrollAnimationPage(int delta) {
        if (delta > 0 && this.animationPage > 0) {
            this.animationPage--;
            this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
            this.initGui();
        }
        if (delta < 0 && this.animationPage < this.maxAnimationPage) {
            this.animationPage++;
            this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
            this.initGui();
        }
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
        scale = MathHelper.clamp_float(tmp, SCALE_MIN, SCALE_MAX);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
