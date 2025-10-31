package com.fox.ysmu.client.renderer.layer;

import com.fox.ysmu.compat.EtfuturumCompat; // 导入你的兼容检查类
import com.fox.ysmu.compat.EtfuturumRendererCompat; // 导入新的兼容类
import com.fox.ysmu.geckolib3.core.IAnimatable;
import com.fox.ysmu.geckolib3.geo.GeoLayerRenderer;
import com.fox.ysmu.geckolib3.geo.IGeoRenderer;
import com.fox.ysmu.geckolib3.geo.render.built.GeoBone;
import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.fox.ysmu.geckolib3.util.RenderUtils;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

public class CustomPlayerElytraLayer<T extends EntityLivingBase & IAnimatable> extends GeoLayerRenderer<T> {
    public CustomPlayerElytraLayer(IGeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }
    @Override

    public void render(T livingEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        // 解决问题2：添加前置检查，如果未安装 Et-Futurum 则直接返回
        if (!EtfuturumCompat.isEtfuturumLoadedLoaded()) {
            return;
        }

        GeoModel geoModel = this.entityRenderer.getGeoModel();
        if (geoModel != null && geoModel.elytraBones != null && !geoModel.elytraBones.isEmpty()) {
            GL11.glPushMatrix();
            this.translateToElytra(geoModel);
            EtfuturumRendererCompat.renderElytra(livingEntity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
            GL11.glPopMatrix();
        }
    }

    protected void translateToElytra(GeoModel geoModel) {
        int size = geoModel.elytraBones.size();
        for (int i = 0; i < size - 1; i++) {
            RenderUtils.prepMatrixForBone(geoModel.elytraBones.get(i));
        }
        GeoBone lastBone = geoModel.elytraBones.get(size - 1);
        RenderUtils.translateMatrixToBone(lastBone);
        RenderUtils.translateToPivotPoint(lastBone);
        RenderUtils.rotateMatrixAroundBone(lastBone);
        RenderUtils.scaleMatrixForBone(lastBone);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
