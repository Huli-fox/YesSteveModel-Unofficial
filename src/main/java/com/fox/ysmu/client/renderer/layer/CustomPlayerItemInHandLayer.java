package com.fox.ysmu.client.renderer.layer;

import net.geckominecraft.client.renderer.GlStateManager;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
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
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.fox.ysmu.compat.BackhandCompat;
import cpw.mods.fml.common.Optional;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.GeoLayerRenderer;
import software.bernie.geckolib3.geo.IGeoRenderer;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.util.RenderUtils;
import xonin.backhand.compat.IOffhandRenderOptOut;

import java.util.List;
import java.util.Objects;

@Optional.Interface(iface = "xonin.backhand.compat.IOffhandRenderOptOut", modid = "backhand")
public class CustomPlayerItemInHandLayer<T extends EntityLivingBase & IAnimatable> extends GeoLayerRenderer<T>
    implements IOffhandRenderOptOut {

    private final ItemRenderer itemRenderer;

    public CustomPlayerItemInHandLayer(IGeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
        this.itemRenderer = new ItemRenderer(Minecraft.getMinecraft());
    }

    @Override
    public void render(T entity, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch, Color renderColor) {
        GeoModel geoModel = entityRenderer.getGeoModel();
        if (geoModel == null) return;

        if (entity instanceof EntityPlayer player) {
            String name = geoModel.properties.getExtraInfo().getName();
            boolean isVanilla = Objects.equals(name, "Steve") || Objects.equals(name, "Alex");
            ItemStack mainHandItem = player.getHeldItem();
            ItemStack offhandItem = BackhandCompat.getOffhandItem(player);
            if (mainHandItem != null || offhandItem != null) {
                GlStateManager.pushMatrix();
                renderArmWithItem(player, mainHandItem, geoModel.rightHandBones, true , isVanilla);
                renderArmWithItem(player, offhandItem, geoModel.leftHandBones, false, isVanilla);
                GlStateManager.popMatrix();
            }
        }
    }

    /**
     * 手部渲染方法
     * @param bones 对应的骨骼列表 (左手或右手)
     * @param isMainHand 是否为主手 (用于镜像处理)
     */
    protected void renderArmWithItem(EntityPlayer player, ItemStack stack, List<GeoBone> bones,
                                     boolean isMainHand, boolean isVanilla) {
        if (stack == null || bones.isEmpty()) return;

        GL11.glPushMatrix();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glDisable(GL11.GL_CULL_FACE);
        if (!isVanilla) {
            GL11.glScalef(0.7F, 0.7F, 0.7F);
        }
        applyBoneTransform(bones);
        if (!isMainHand) {
            GL11.glScalef(-1, 1, 1);
            GL11.glFrontFace(GL11.GL_CW); // 修正镜像导致的面剔除反转
        }

        doRenderItem(player, stack);

        if (!isMainHand) {
            GL11.glFrontFace(GL11.GL_CCW);
        }
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    protected void applyBoneTransform(List<GeoBone> bones) {
        int size = bones.size();
        for (int i = 0; i < size - 1; i++) {
            RenderUtils.prepMatrixForBone(bones.get(i));
        }
        GeoBone lastBone = bones.get(size - 1);
        RenderUtils.translateMatrixToBone(lastBone);
        RenderUtils.translateToPivotPoint(lastBone);
        RenderUtils.rotateMatrixAroundBone(lastBone);
        RenderUtils.scaleMatrixForBone(lastBone);
    }

    private void doRenderItem(EntityPlayer player, ItemStack itemInHand) {
        // 鱼竿显示为木棍的特殊逻辑
        if (player.fishEntity != null && itemInHand.getItem() == Items.fishing_rod) {
            itemInHand = new ItemStack(Items.stick);
        }

        IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(itemInHand, IItemRenderer.ItemRenderType.EQUIPPED);
        boolean is3D = (customRenderer != null && customRenderer.shouldUseRenderHelper(IItemRenderer.ItemRenderType.EQUIPPED, itemInHand, IItemRenderer.ItemRendererHelper.BLOCK_3D));
        boolean isBlock = itemInHand.getItem() instanceof ItemBlock && RenderBlocks.renderItemIn3d(Block.getBlockFromItem(itemInHand.getItem()).getRenderType());
        if (is3D || isBlock) {
            GL11.glTranslatef(0.0F, 0.0F, -0.3125F);
            GL11.glRotatef(-20.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            float f2 = 0.375F; // 0.5 * 0.75
            GL11.glScalef(f2, f2, -f2);
        } else if (itemInHand.getItem() == Items.bow) {
            GL11.glTranslatef(-0.1F, 0.125F, 0.3125F);
            GL11.glRotatef(-15.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.625F, 0.625F, 0.625F);
            GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
        } else if (itemInHand.getItem().isFull3D()) {
            if (itemInHand.getItem().shouldRotateAroundWhenRendering()) {
                GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
                GL11.glTranslatef(0.0F, -0.125F, 0.0F);
            }
            if (player.getItemInUseCount() > 0 && itemInHand.getItemUseAction() == EnumAction.block) {
                GL11.glTranslatef(0.05F, 0.0F, -0.1F);
                GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(-60.0F, 0.0F, 0.0F, 1.0F);
            }
            GL11.glTranslatef(0.0F, -0.1F, 0.0F);
            GL11.glScalef(0.625F, 0.625F, 0.625F);
            GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
        } else {
            GL11.glTranslatef(0.175F, 0.0875F, -0.15F);
            GL11.glScalef(0.375F, 0.375F, 0.375F);
            GL11.glRotatef(60.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(-60.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);
        }

        // 统一处理单层和多层渲染
        int passes = 1;
        if (itemInHand.getItem().requiresMultipleRenderPasses()) {
            passes = itemInHand.getItem().getRenderPasses(itemInHand.getItemDamage());
        }
        for (int k = 0; k < passes; ++k) {
            int color = itemInHand.getItem().getColorFromItemStack(itemInHand, k);
            float r = (float) (color >> 16 & 255) / 255.0F;
            float g = (float) (color >> 8 & 255) / 255.0F;
            float b = (float) (color & 255) / 255.0F;
            GL11.glColor4f(r, g, b, 1.0F);
            itemRenderer.renderItem(player, itemInHand, k);
        }
    }
}
