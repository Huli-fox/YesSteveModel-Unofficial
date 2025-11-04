package net.geckominecraft.client.renderer.entity.layers;

import net.minecraft.entity.EntityLivingBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface LayerRenderer<E extends EntityLivingBase> {

    void doRenderLayer(E entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks,
        float ageInTicks, float netHeadYaw, float headPitch, float scale);

    boolean shouldCombineTextures();
}
