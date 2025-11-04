package com.fox.ysmu.client.renderer.layer;

import net.geckominecraft.client.renderer.GlStateManager;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.vector.Quaternion;

import com.fox.ysmu.client.ClientEventHandler;
import com.fox.ysmu.compat.Axis;
import com.fox.ysmu.compat.BackhandCompat;
import com.fox.ysmu.compat.Utils;

import cpw.mods.fml.common.Optional;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.GeoLayerRenderer;
import software.bernie.geckolib3.geo.IGeoRenderer;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.util.RenderUtils;
import xonin.backhand.compat.IOffhandRenderOptOut;

@Optional.Interface(iface = "xonin.backhand.compat.IOffhandRenderOptOut", modid = "backhand")
public class CustomPlayerItemInHandLayer<T extends EntityLivingBase & IAnimatable> extends GeoLayerRenderer<T>
    implements IOffhandRenderOptOut {

    private final ItemRenderer itemRenderer;

    public CustomPlayerItemInHandLayer(IGeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
        this.itemRenderer = new ItemRenderer(Minecraft.getMinecraft());
    }

    @Override
    public void render(T entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTicks,
        float ageInTicks, float netHeadYaw, float headPitch, Color renderColor) {
        if (entityRenderer.getGeoModel() == null) {
            return;
        }
        if (entityLivingBaseIn instanceof EntityPlayer player) {
            ItemStack offhandItem = BackhandCompat.getOffhandItem(player);
            ItemStack mainHandItem = player.getHeldItem();
            GeoModel geoModel = entityRenderer.getGeoModel();
            if (offhandItem != null || mainHandItem != null) {
                GlStateManager.pushMatrix();
                if (!geoModel.rightHandBones.isEmpty()) {
                    this.renderArmWithItem(player, mainHandItem, true);
                }
                if (!geoModel.leftHandBones.isEmpty()) {
                    this.renderArmWithItem(player, offhandItem, false);
                }
                GlStateManager.popMatrix();
            }
        }
    }

    protected void renderArmWithItem(EntityPlayer player, ItemStack itemStack, boolean isMainHand) {
        ModelBiped biped = ClientEventHandler.getModelBiped();
        if (itemStack != null && this.entityRenderer.getGeoModel() != null) {
            if (isMainHand) {
                GL11.glPushMatrix();
                GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                GL11.glDisable(GL11.GL_CULL_FACE);
                renderMainhandItemIn3rdPerson(player, biped, itemStack);
                GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glPopMatrix();
            } else {
                GL11.glPushMatrix();
                GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                GL11.glDisable(GL11.GL_CULL_FACE);
                renderOffhandItemIn3rdPerson(player, biped, itemStack);
                GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glPopMatrix();
            }
        }
    }

    protected void translateToHand(boolean isMainHand, GeoModel geoModel) {
        if (!isMainHand) {
            int size = geoModel.leftHandBones.size();
            for (int i = 0; i < size - 1; i++) {
                RenderUtils.prepMatrixForBone(geoModel.leftHandBones.get(i));
            }
            GeoBone lastBone = geoModel.leftHandBones.get(size - 1);
            RenderUtils.translateMatrixToBone(lastBone);
            RenderUtils.translateToPivotPoint(lastBone);
            RenderUtils.rotateMatrixAroundBone(lastBone);
            RenderUtils.scaleMatrixForBone(lastBone);
        } else {
            int size = geoModel.rightHandBones.size();
            for (int i = 0; i < size - 1; i++) {
                RenderUtils.prepMatrixForBone(geoModel.rightHandBones.get(i));
            }
            GeoBone lastBone = geoModel.rightHandBones.get(size - 1);
            RenderUtils.translateMatrixToBone(lastBone);
            RenderUtils.translateToPivotPoint(lastBone);
            RenderUtils.rotateMatrixAroundBone(lastBone);
            RenderUtils.scaleMatrixForBone(lastBone);
        }
    }

    protected void renderMainhandItemIn3rdPerson(EntityPlayer player, ModelBiped modelBipedMain,
        ItemStack mainhandItem) {
        float f2;
        float f4;
        // TODO 很多数值取反了，待检查
        if (mainhandItem != null) {
            GL11.glPushMatrix();
            modelBipedMain.bipedRightArm.postRender(0.0625F);
            GL11.glTranslatef(
                -modelBipedMain.bipedRightArm.rotationPointX * 0.0625F,
                -modelBipedMain.bipedRightArm.rotationPointY * 0.0625F,
                -modelBipedMain.bipedRightArm.rotationPointZ * 0.0625F);
            GL11.glTranslatef(
                -modelBipedMain.bipedRightArm.rotationPointX * 0.0625F,
                modelBipedMain.bipedRightArm.rotationPointY * 0.0625F,
                -modelBipedMain.bipedRightArm.rotationPointZ * 0.0625F);

            GL11.glTranslatef(0.0625F, 0.4375F, 0.0625F);

            if (player.fishEntity != null && mainhandItem.getItem() == Items.fishing_rod) {
                mainhandItem = new ItemStack(Items.stick);
            }

            EnumAction enumaction = null;

            if (player.getItemInUseCount() > 0) {
                enumaction = mainhandItem.getItemUseAction();
            }

            IItemRenderer customRenderer = MinecraftForgeClient
                .getItemRenderer(mainhandItem, IItemRenderer.ItemRenderType.EQUIPPED);

            boolean is3D = (customRenderer != null && customRenderer.shouldUseRenderHelper(
                IItemRenderer.ItemRenderType.EQUIPPED,
                mainhandItem,
                IItemRenderer.ItemRendererHelper.BLOCK_3D));

            if (is3D || mainhandItem.getItem() instanceof ItemBlock && RenderBlocks.renderItemIn3d(
                Block.getBlockFromItem(mainhandItem.getItem())
                    .getRenderType())) {
                f2 = 0.5F;
                GL11.glTranslatef(0.0F, 0.1875F, 0.3125F);
                f2 *= 0.75F;
                GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(-f2, -f2, f2);
            } else if (mainhandItem.getItem() == Items.bow) {
                f2 = 0.625F;
                GL11.glTranslatef(0.0F, 0.125F, -0.3125F);
                GL11.glRotatef(-20.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(f2, -f2, f2);
                GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            } else if (mainhandItem.getItem()
                .isFull3D()) {
                    f2 = 0.625F;

                    if (mainhandItem.getItem()
                        .shouldRotateAroundWhenRendering()) {
                        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
                        GL11.glTranslatef(0.0F, -0.125F, 0.0F);
                    }

                    if (player.getItemInUseCount() > 0 && enumaction == EnumAction.block) {
                        GL11.glTranslatef(0.05F, 0.0F, -0.1F);
                        GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
                        GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
                        GL11.glRotatef(-60.0F, 0.0F, 0.0F, 1.0F);
                    }

                    GL11.glTranslatef(0.0F, 0.1875F, 0.0F);
                    GL11.glScalef(f2, -f2, f2); // 镜像变换？需要修改
                    GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
                } else {
                    f2 = 0.375F;
                    GL11.glTranslatef(-0.25F, 0.1875F, 0.1875F);
                    GL11.glScalef(f2, f2, f2);
                    GL11.glRotatef(60.0F, 0.0F, 0.0F, 1.0F);
                    GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);
                }

            float f3;
            int k;
            float f12;

            if (mainhandItem.getItem()
                .requiresMultipleRenderPasses()) {
                for (k = 0; k < mainhandItem.getItem()
                    .getRenderPasses(mainhandItem.getItemDamage()); ++k) {
                    int i = mainhandItem.getItem()
                        .getColorFromItemStack(mainhandItem, k);
                    f12 = (float) (i >> 16 & 255) / 255.0F;
                    f3 = (float) (i >> 8 & 255) / 255.0F;
                    f4 = (float) (i & 255) / 255.0F;
                    GL11.glColor4f(f12, f3, f4, 1.0F);
                    translateToHand(true, this.entityRenderer.getGeoModel());
                    GlStateManager.translate(0, -0.0625, -0.1);
                    GlStateManager.rotate(j2l(Axis.XP.rotationDegrees(-90.0F)));
                    itemRenderer.renderItem(player, mainhandItem, k);
                }
            } else {
                k = mainhandItem.getItem()
                    .getColorFromItemStack(mainhandItem, 0);
                float f11 = (float) (k >> 16 & 255) / 255.0F;
                f12 = (float) (k >> 8 & 255) / 255.0F;
                f3 = (float) (k & 255) / 255.0F;
                GL11.glColor4f(f11, f12, f3, 1.0F);
                translateToHand(true, this.entityRenderer.getGeoModel());
                GlStateManager.translate(0, -0.0625, -0.1);
                GlStateManager.rotate(j2l(Axis.XP.rotationDegrees(-90.0F)));
                itemRenderer.renderItem(player, mainhandItem, 0);
            }

            GL11.glFrontFace(GL11.GL_CCW);
            GL11.glPopMatrix();
        }
    }

    protected void renderOffhandItemIn3rdPerson(EntityPlayer player, ModelBiped modelBipedMain, ItemStack offhandItem) {
        float f2;
        float f4;

        if (offhandItem != null) {
            GL11.glPushMatrix();
            modelBipedMain.bipedLeftArm.postRender(0.0625F);
            GL11.glTranslatef(
                -modelBipedMain.bipedLeftArm.rotationPointX * 0.0625F,
                -modelBipedMain.bipedLeftArm.rotationPointY * 0.0625F,
                -modelBipedMain.bipedLeftArm.rotationPointZ * 0.0625F);
            GL11.glScalef(-1, 1, 1);
            GL11.glTranslatef(
                -modelBipedMain.bipedLeftArm.rotationPointX * 0.0625F,
                modelBipedMain.bipedLeftArm.rotationPointY * 0.0625F,
                -modelBipedMain.bipedLeftArm.rotationPointZ * 0.0625F);

            GL11.glTranslatef(-0.0625F, 0.4375F, 0.0625F);

            GL11.glFrontFace(GL11.GL_CW);

            if (player.fishEntity != null && offhandItem.getItem() == Items.fishing_rod) {
                offhandItem = new ItemStack(Items.stick);
            }

            EnumAction enumaction = null;

            if (player.getItemInUseCount() > 0) {
                enumaction = offhandItem.getItemUseAction();
            }

            IItemRenderer customRenderer = MinecraftForgeClient
                .getItemRenderer(offhandItem, IItemRenderer.ItemRenderType.EQUIPPED);

            boolean is3D = (customRenderer != null && customRenderer.shouldUseRenderHelper(
                IItemRenderer.ItemRenderType.EQUIPPED,
                offhandItem,
                IItemRenderer.ItemRendererHelper.BLOCK_3D));

            if (is3D || offhandItem.getItem() instanceof ItemBlock && RenderBlocks.renderItemIn3d(
                Block.getBlockFromItem(offhandItem.getItem())
                    .getRenderType())) {
                f2 = 0.5F;
                GL11.glTranslatef(0.0F, 0.1875F, -0.3125F);
                f2 *= 0.75F;
                GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(-f2, -f2, f2);
            } else if (offhandItem.getItem() == Items.bow) {
                f2 = 0.625F;
                GL11.glTranslatef(0.0F, 0.125F, 0.3125F);
                GL11.glRotatef(-20.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(f2, -f2, f2);
                GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            } else if (offhandItem.getItem()
                .isFull3D()) {
                    f2 = 0.625F;

                    if (offhandItem.getItem()
                        .shouldRotateAroundWhenRendering()) {
                        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
                        GL11.glTranslatef(0.0F, -0.125F, 0.0F);
                    }

                    if (player.getItemInUseCount() > 0 && enumaction == EnumAction.block) {
                        GL11.glTranslatef(0.05F, 0.0F, -0.1F);
                        GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
                        GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
                        GL11.glRotatef(-60.0F, 0.0F, 0.0F, 1.0F);
                    }

                    GL11.glTranslatef(0.0F, 0.1875F, 0.0F);
                    GL11.glScalef(f2, -f2, f2);
                    GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
                } else {
                    f2 = 0.375F;
                    GL11.glTranslatef(0.25F, 0.1875F, -0.1875F);
                    GL11.glScalef(f2, f2, f2);
                    GL11.glRotatef(60.0F, 0.0F, 0.0F, 1.0F);
                    GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);
                }

            float f3;
            int k;
            float f12;

            if (offhandItem.getItem()
                .requiresMultipleRenderPasses()) {
                for (k = 0; k < offhandItem.getItem()
                    .getRenderPasses(offhandItem.getItemDamage()); ++k) {
                    int i = offhandItem.getItem()
                        .getColorFromItemStack(offhandItem, k);
                    f12 = (float) (i >> 16 & 255) / 255.0F;
                    f3 = (float) (i >> 8 & 255) / 255.0F;
                    f4 = (float) (i & 255) / 255.0F;
                    GL11.glColor4f(f12, f3, f4, 1.0F);
                    translateToHand(false, this.entityRenderer.getGeoModel());
                    GlStateManager.translate(0, -0.0625, -0.1);
                    GlStateManager.rotate(j2l(Axis.XP.rotationDegrees(-90.0F)));
                    itemRenderer.renderItem(player, offhandItem, k);
                }
            } else {
                k = offhandItem.getItem()
                    .getColorFromItemStack(offhandItem, 0);
                float f11 = (float) (k >> 16 & 255) / 255.0F;
                f12 = (float) (k >> 8 & 255) / 255.0F;
                f3 = (float) (k & 255) / 255.0F;
                GL11.glColor4f(f11, f12, f3, 1.0F);
                translateToHand(false, this.entityRenderer.getGeoModel());
                GlStateManager.translate(0, -0.0625, -0.1);
                GlStateManager.rotate(j2l(Axis.XP.rotationDegrees(-90.0F)));
                itemRenderer.renderItem(player, offhandItem, 0);
            }

            GL11.glFrontFace(GL11.GL_CCW);
            GL11.glPopMatrix();
        }
    }

    private static Quaternion j2l(Quaternionf jomlQuat) {
        return Utils.j2l(jomlQuat);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
