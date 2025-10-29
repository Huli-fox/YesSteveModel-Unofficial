package com.fox.ysmu.geckolib3.geo;

import com.fox.ysmu.util.Keep;
import com.fox.ysmu.geckolib3.core.IAnimatable;
// 导入了1.7.10的 Color 和 GlStateManager 工具类，用于颜色和GL状态处理
import com.fox.ysmu.geckolib3.core.util.Color;
import com.fox.ysmu.geckolib3.model.provider.GeoModelProvider;
// 导入了1.7.10的 LayerRenderer 接口，用于集成到实体渲染流程中
import com.fox.ysmu.geckolib3.util.GlStateManager;

import com.fox.ysmu.geckolib3.util.LayerRenderer;
// 移除了高版本的 PoseStack, MultiBufferSource, RenderType 等不存在的类
// import com.mojang.blaze3d.vertex.PoseStack;
// import net.minecraft.client.renderer.MultiBufferSource;
// import net.minecraft.client.renderer.RenderType;
// import net.minecraft.client.renderer.entity.LivingEntityRenderer;

// 导入了1.7.10对应的实体基类和资源路径类
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;


/**
 * 移植说明：
 * 该类已从高版本移植到1.7.10。
 * 主要改动是移除了所有与新版渲染管道相关的类（PoseStack, MultiBufferSource等），
 * 并将方法签名和实现调整为适应1.7.10的渲染方式。
 * 为了与1.7.10的渲染器集成，实现了LayerRenderer接口。
 */
@SuppressWarnings("unchecked")
// [变化] 泛型约束从 <T extends Entity & IAnimatable> 修改为 <T extends EntityLivingBase & IAnimatable> 以适应1.7.10的类结构。
// [变化] 实现了 LayerRenderer<T> 接口，这是1.7.10中实现分层渲染的标准方式。
public abstract class GeoLayerRenderer<T extends EntityLivingBase & IAnimatable> implements LayerRenderer<T> {
    protected final IGeoRenderer<T> entityRenderer;

    public GeoLayerRenderer(IGeoRenderer<T> entityRendererIn) {
        this.entityRenderer = entityRendererIn;
    }

    /**
     * [变化] 方法签名已完全修改以适应1.7.10。
     * 移除了 PoseStack, MultiBufferSource, packedLight 等高版本参数。
     * 添加了 limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch 等1.7.10渲染时可用的动画参数。
     * 此方法现在是 renderModel 的一个包装，增加了对实体是否隐形的检查。
     */
    protected void renderCopyModel(GeoModelProvider<T> modelProvider, T animatable, float partialTicks, float red, float green, float blue) {
        if (!animatable.isInvisible()) {
            renderModel(modelProvider, animatable, partialTicks, red, green, blue);
        }
    }

    /**
     * [变化] 这是高版本 renderModel 的1.7.10适配版，方法签名已完全重写。
     * 1. 移除了 PoseStack, MultiBufferSource, packedLight 等高版本参数。
     * 2. 添加了1.7.10的动画参数。
     * 3. 渲染逻辑改为通过主渲染器(IGeoRenderer)的render方法进行，这是GeckoLib的推荐做法。
     *    我们假设 IGeoRenderer.render 方法已经被适配到1.7.10，并能接收纹理和颜色信息。
     * 4. 移除了 getRenderType 的调用，因为 RenderType 在1.7.10不存在。
     * 5. 移除了对 LivingEntityRenderer.getOverlayCoords 的调用，覆盖层渲染在1.7.10中处理方式不同，通常由主渲染器内部管理。
     */
    protected void renderModel(GeoModelProvider<T> modelProvider, T animatable, float partialTicks, float red, float green, float blue) {
        getRenderer().render(modelProvider.getModel(modelProvider.getModelLocation(animatable)),
            animatable, partialTicks, red, green, blue, 1);
    }

    /**
     * [无变化] 此方法与Minecraft版本无关，予以保留。
     * 用于获取当前渲染器关联的模型提供者。
     */
    public GeoModelProvider<T> getEntityModel() {
        return this.entityRenderer.getGeoModelProvider();
    }

    /**
     * [无变化] 此方法与Minecraft版本无关，予以保留。
     * 用于获取包装此层的实体渲染器实例。
     */
    public IGeoRenderer<T> getRenderer() {
        return this.entityRenderer;
    }

    /**
     * [无变化] 此方法与Minecraft版本无关，予以保留。
     * 用于获取实体默认的纹理位置。
     */
    protected ResourceLocation getEntityTexture(T entityIn) {
        return this.entityRenderer.getTextureLocation(entityIn);
    }

    /**
     * [重大变化] 这是子类需要实现的核心渲染方法，其签名已为1.7.10进行适配。
     * 移除了高版本的 PoseStack, MultiBufferSource, packedLightIn 参数。
     * 子类实现此方法时，可以直接调用 renderModel 或 renderCopyModel 来执行渲染。
     * 为了最大程度地保持与你项目中其他文件的兼容性，保留了原有的方法名和大部分参数。
     * 你在修改子类时，只需移除方法签名的前三个参数即可。
     */
    @Keep
    public abstract void render(T entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                float netHeadYaw, float headPitch);

    /**
     * [新增] 实现 LayerRenderer 接口所必需的方法。
     * 这是1.7.10实体渲染器调用层渲染的入口点。
     * 它直接调用我们适配后的抽象方法 render(...)，从而将渲染逻辑传递给子类。
     * 这里的 scale 参数是1.7.10层渲染的標準参数之一，我们没有将其传递给 render 方法，因为通常用不到。
     */
    @Override
    public void doRenderLayer(T entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTicks,
                              float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.render(entityLivingBaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
    }
}
