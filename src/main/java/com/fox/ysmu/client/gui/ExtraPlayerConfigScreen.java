//package com.fox.ysmu.client.gui;
//
//import com.fox.ysmu.Config;
//import com.fox.ysmu.util.RenderUtil;
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.network.chat.Component;
//import net.minecraft.network.chat.MutableComponent;
//import net.minecraft.util.FormattedCharSequence;
//import org.lwjgl.glfw.GLFW;
//
//import java.util.List;
//
//public class ExtraPlayerConfigScreen extends Screen {
//    private static final char RESET_KEY = 'r';
//    private int posX;
//    private int posY;
//    private float scale;
//    private float yawOffset;
//    private boolean isChangePos = false;
//    private boolean isChangeScale = false;
//
//    public ExtraPlayerConfigScreen() {
//        super(Component.literal("YSM Extra Player Render Config GUI"));
//        this.posX = Config.PLAYER_POS_X;
//        this.posY = Config.PLAYER_POS_Y;
//        this.scale = (float) Config.PLAYER_SCALE;
//        this.yawOffset = (float) Config.PLAYER_YAW_OFFSET;
//    }
//
//
//    @Override
//    public void drawScreen(int pMouseX, int pMouseY, float pPartialTick) {
//        int startX = this.posX;
//        int startY = this.posY;
//        int endX = (int) (startX + this.scale * 1);
//        int endY = (int) (startY + this.scale * 2);
//
//        graphics.vLine(width / 2 - 1, -2, height + 2, 0x9fffffff);
//        graphics.hLine(-2, width + 2, height / 2 - 1, 0x9fffffff);
//
//        graphics.vLine(10, -2, height + 2, 0x9fffffff);
//        graphics.vLine(width - 10, -2, height + 2, 0x9fffffff);
//        graphics.hLine(-2, width + 2, 10, 0x9fffffff);
//        graphics.hLine(-2, width + 2, height - 10, 0x9fffffff);
//
//        graphics.vLine(startX, startY, endY, 0xffff0000);
//        graphics.vLine(endX, startY, endY, 0xffff0000);
//        graphics.hLine(startX, endX, startY, 0xffff0000);
//        graphics.hLine(startX, endX, endY, 0xffff0000);
//
//        graphics.fillGradient(startX, startY, endX, endY, 0x4fffffff, 0x4fffffff);
//
//        graphics.fillGradient(startX - 5, startY - 5, startX + 5, startY + 5, 0xFF00FF9F, 0xFF00FF9F);
//        graphics.fillGradient(endX - 5, endY - 5, endX + 5, endY + 5, 0xFF00009F, 0xFF00009F);
//
//        int y = 15;
//        MutableComponent component = I18n.format("gui.yes_steve_model.extra_player_render.tips");
//        List<FormattedCharSequence> split = font.split(component, 500);
//        for (FormattedCharSequence charSequence : split) {
//            int w = font.width(charSequence);
//            graphics.drawString(font, charSequence, width - 15 - w, y, 0xFFFFFF);
//            y += 10;
//        }
//
//        if (getMinecraft().player != null) {
//            RenderUtil.renderPlayerEntity(getMinecraft().player, this.posX, this.posY, this.scale, this.yawOffset, 50);
//        }
//    }
//
//
//    @Override
//    public boolean mouseClicked(double mouseX, double mouseY, int button) {
//        boolean xIn = this.posX - 5 < mouseX && mouseX < this.posX + 5;
//        boolean yIn = this.posY - 5 < mouseY && mouseY < this.posY + 5;
//        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && xIn && yIn) {
//            this.isChangePos = true;
//        }
//        int endX = (int) (this.posX + this.scale * 1);
//        int endY = (int) (this.posY + this.scale * 2);
//        boolean xIn2 = endX - 5 < mouseX && mouseX < endX + 5;
//        boolean yIn2 = endY - 5 < mouseY && mouseY < endY + 5;
//        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && xIn2 && yIn2) {
//            this.isChangeScale = true;
//        }
//        return super.mouseClicked(mouseX, mouseY, button);
//    }
//
//
//    @Override
//    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
//        this.isChangePos = false;
//        this.isChangeScale = false;
//        return super.mouseReleased(pMouseX, pMouseY, pButton);
//    }
//
//
//    @Override
//    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
//        if (isChangeScale) {
//            double scale1 = mouseX - this.posX;
//            double scale2 = (mouseY - this.posY) / 2;
//            this.scale = (float) Math.min(scale1, scale2);
//            return true;
//        }
//        if (isChangePos) {
//            this.posX = (int) mouseX;
//            this.posY = (int) mouseY;
//            return true;
//        }
//        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
//            this.yawOffset += (deltaX * 2);
//            return true;
//        }
//        return false;
//    }
//
//
//    @Override
//    public boolean charTyped(char typedChar, int keyCode) {
//        if (Character.toLowerCase(typedChar) == RESET_KEY && hasAltDown()) {
//            this.posX = 10;
//            this.posY = 10;
//            this.scale = 40;
//            this.yawOffset = 5;
//        }
//        return super.charTyped(typedChar, keyCode);
//    }
//
//
//    @Override
//    public void onClose() {
//        Config.PLAYER_POS_X = this.posX;
//        Config.PLAYER_POS_Y = this.posY;
//        Config.PLAYER_SCALE = this.scale;
//        Config.PLAYER_YAW_OFFSET = this.yawOffset;
//        super.onClose();
//    }
//}
