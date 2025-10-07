package com.fox.test.client.renderer.entity;

import com.fox.test.client.model.entity.CustomEntityModel;
import com.fox.test.entity.CustomEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class CustomEntityRenderer extends GeoEntityRenderer<CustomEntity> {

    public CustomEntityRenderer() {
        // 将我们之前创建的模型类传递给渲染器
        super(new CustomEntityModel());
        // 设置实体的阴影大小
        this.shadowSize = 0.5F;
    }

    /**
     * 这是为了满足 Minecraft 1.7.10 基类 Render 的要求而必须实现的方法。
     * 我们让它直接调用 Geckolib 模型中定义的纹理路径。
     */
    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return this.modelProvider.getTextureLocation((CustomEntity) entity);
    }
    
    // 移除了错误的@Override注解和doRender方法，因为父类已经提供了正确的实现
}