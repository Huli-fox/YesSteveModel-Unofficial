package com.fox.ysmu.client.renderer.entity;

import net.minecraft.entity.Entity; // 新增导入
import net.minecraft.util.ResourceLocation; // 新增导入
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
import com.fox.ysmu.client.model.entity.ModelDisguiseGecko;
import com.fox.ysmu.entity.EntityDisguiseGecko;


public class RenderDisguiseGecko extends GeoEntityRenderer<EntityDisguiseGecko> {

    public RenderDisguiseGecko() {
        super(new ModelDisguiseGecko());
    }

    /**
     * 【修正】覆盖基类 'Render' 的方法，使用正确的字段 'modelProvider'。
     * 访问修饰符应为 protected 以匹配父类。
     */
    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        // 调用我们模型提供者中的 getTextureLocation 方法
        return this.modelProvider.getTextureLocation((EntityDisguiseGecko) entity);
    }
}