package com.fox.ysmu.client.gui;

import com.fox.ysmu.client.gui.button.FlatColorButton;
import com.fox.ysmu.client.gui.button.TextureButton;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.util.Comparator;
import java.util.List;

public class PlayerTextureScreen extends GuiScreen {
    private static final int TEXTURES_PER_PAGE = 14;
    private static final int TEXTURE_COLUMNS = 7;
    private static final int TEXTURE_X_STEP = 58;
    private static final int TEXTURE_Y_STEP = 102;

    private final PlayerModelScreen parent;
    private final ResourceLocation modelId;
    private final List<ResourceLocation> textures;
    private final EntityPlayer player;
    private int maxTexturePage;
    private int texturePage;
    private int x;
    private int y;

    public PlayerTextureScreen(PlayerModelScreen parent, ResourceLocation modelId, List<ResourceLocation> textures) {
        this.parent = parent;
        this.modelId = modelId;
        this.textures = textures;
        this.textures.sort(Comparator.comparing(ResourceLocation::toString));
        this.player = parent.player;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.x = (width - 420) / 2;
        this.y = (height - 235) / 2;
        this.maxTexturePage = textures.isEmpty() ? 0 : (textures.size() - 1) / TEXTURES_PER_PAGE;
        if (this.texturePage > this.maxTexturePage) {
            this.texturePage = 0;
        }

        int b = 30;
        this.buttonList.add(new FlatColorButton(0, x + 5, y + 5, 80, 18, I18n.format("gui.yes_steve_model.model.return")));
        this.buttonList.add(new FlatColorButton(1, x + 198, y + 7, 52, 14, I18n.format("gui.yes_steve_model.pre_page")));
        this.buttonList.add(new FlatColorButton(2, x + 308, y + 7, 52, 14, I18n.format("gui.yes_steve_model.next_page")));

        for (int i = 0; i < TEXTURES_PER_PAGE; i++) {
            int modelIndex = i + this.texturePage * TEXTURES_PER_PAGE;
            if (modelIndex >= textures.size()) {
                break;
            }
            int xStart = x + 5 + TEXTURE_X_STEP * (i % TEXTURE_COLUMNS);
            int yStart = y + 28 + TEXTURE_Y_STEP * (i / TEXTURE_COLUMNS);
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
                if (this.texturePage > 0) {
                    this.texturePage--;
                    this.initGui();
                }
                break;
            case 2:
                if (this.texturePage < this.maxTexturePage) {
                    this.texturePage++;
                    this.initGui();
                }
                break;
            default:
                if (button instanceof TextureButton) {
                    ((TextureButton) button).doPress();
                }
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        this.drawDefaultBackground();
        this.drawGradientRect(x, y, x + 420, y + 235, 0xff_222222, 0xff_222222);

        String texturePageInfo = String.format("%d/%d", texturePage + 1, this.maxTexturePage + 1);
        this.drawString(fontRendererObj, texturePageInfo, x + 251 + (56 - fontRendererObj.getStringWidth(texturePageInfo)) / 2, y + 10, 0xF3EFE0);

        super.drawScreen(mouseX, mouseY, partialTick);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
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

    private boolean inTextureRange(double mouseX, double mouseY) {
        boolean isInWidthRange = x < mouseX && mouseX < (x + 420);
        boolean isInHeightRange = (y + 25) < mouseY && mouseY < (y + 235);
        return isInWidthRange && isInHeightRange;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
