package com.fox.ysmu.compat; // 建议放在一个专门的compat包里

import ganymedes01.etfuturum.client.renderer.entity.elytra.ModelElytra;
import ganymedes01.etfuturum.items.equipment.ItemArmorElytra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

// 这个类只在EtfuturumCompat确认mod已加载后才会被调用
public class EtfuturumRendererCompat {

    private static final ResourceLocation WINGS_LOCATION = new ResourceLocation("textures/entity/elytra.png");
    private static final ResourceLocation ENCHANTED_ITEM_GLINT_RES = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    private static final ModelElytra modelElytra = new ModelElytra();

    public static void renderElytra(EntityLivingBase livingEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemStack stack = ItemArmorElytra.getElytra(livingEntity);
        if (stack == null) {
            return;
        }

        ResourceLocation texture;
        if (livingEntity instanceof AbstractClientPlayer) {
            AbstractClientPlayer player = (AbstractClientPlayer) livingEntity;
            if (player.func_152122_n() && player.getLocationCape() != null) {
                texture = player.getLocationCape();
            } else {
                texture = WINGS_LOCATION;
            }
        } else {
            texture = WINGS_LOCATION;
        }

        GL11.glColor3f(1.0F, 1.0F, 1.0F);

        // --- 坐标变换 ---
        // 这些值可能需要根据你的模型和1.7.10的坐标系进行微调
        GL11.glTranslatef(0.0F, -1.5F, 0.0F);
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
        // 如果觉得鞘翅太小或太大，可以取消这行注释并调整数值
        // GL11.glScalef(1.0f, 1.0f, 1.0f);

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        modelElytra.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, livingEntity);
        modelElytra.render(livingEntity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

        if (stack.isItemEnchanted()) {
            renderGlint(livingEntity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
        }
    }

    private static void renderGlint(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        float f = (float)entitylivingbaseIn.ticksExisted + partialTicks;
        Minecraft.getMinecraft().renderEngine.bindTexture(ENCHANTED_ITEM_GLINT_RES);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthFunc(GL11.GL_EQUAL);
        GL11.glDepthMask(false);
        float f1 = 0.5F;
        GL11.glColor4f(f1, f1, f1, 1.0F);

        for (int i = 0; i < 2; ++i) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
            float f2 = 0.76F;
            GL11.glColor4f(0.5F * f2, 0.25F * f2, 0.8F * f2, 1.0F);
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glLoadIdentity();
            float f3 = 0.33333334F;
            GL11.glScalef(f3, f3, f3);
            GL11.glRotatef(30.0F - (float)i * 60.0F, 0.0F, 0.0F, 1.0F);
            GL11.glTranslatef(0.0F, f * (0.001F + (float)i * 0.003F) * 20.0F, 0.0F);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            modelElytra.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDepthMask(true);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
