package com.fox.ysmu.geckolib3.geo;

import com.google.common.collect.Lists;
import com.fox.ysmu.mclib.utils.Interpolations;
import com.fox.ysmu.geckolib3.core.IAnimatable;
import com.fox.ysmu.geckolib3.core.IAnimatableModel;
import com.fox.ysmu.geckolib3.core.controller.AnimationController;
import com.fox.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.fox.ysmu.geckolib3.core.util.Color;
import com.fox.ysmu.geckolib3.geo.RenderHurtColor;
import com.fox.ysmu.geckolib3.geo.render.built.GeoBone;
import com.fox.ysmu.geckolib3.geo.render.built.GeoCube;
import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.fox.ysmu.geckolib3.model.AnimatedGeoModel;
import com.fox.ysmu.geckolib3.model.DummyVanilaModel;
import com.fox.ysmu.geckolib3.model.provider.GeoModelProvider;
import com.fox.ysmu.geckolib3.model.provider.data.EntityModelData;
import com.fox.ysmu.geckolib3.util.AnimationUtils;
import com.fox.ysmu.geckolib3.util.GlStateManager;
import com.fox.ysmu.geckolib3.util.RenderUtils;
import com.fox.ysmu.util.Keep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;


@SuppressWarnings({"rawtypes", "unchecked"})
// 修改：将基类从高版本的 EntityRenderer<T> 修改为1.7.10的 RendererLivingEntity。
// 修改：泛型约束从 LivingEntity 修改为 EntityLivingBase。
public abstract class GeoEntityRenderer<T extends EntityLivingBase & IAnimatable> extends RendererLivingEntity implements IGeoRenderer<T> {
    // 修改：使用旧版的 lambda 语法，以兼容 Java 8。
    static {
        AnimationController.addModelFetcher((IAnimatable object) -> {
            if (object instanceof Entity) {
                return (IAnimatableModel<Object>) AnimationUtils.getGeoModelForEntity((Entity) object);
            }
            return null;
        });
    }

    protected final AnimatedGeoModel<T> modelProvider;
    // 修改：将 ObjectArrayList 替换为 Guava 的 Lists.newArrayList()，更符合 1.7.10 的习惯。
    protected final List<GeoLayerRenderer<T>> layerRenderers = Lists.newArrayList();

    // 修改：移除了高版本特有的字段，如 MultiBufferSource, PoseStack相关的矩阵, 以及装备物品缓存。
    // 这些在高版本的 renderEarly 方法中被赋值，但在1.7.10的渲染流程中不需要。
    protected T animatable;
    protected float widthScale = 1;
    protected float heightScale = 1;

    /**
     * 修改：构造函数已完全替换为1.7.10版本。
     * 它不再接收 EntityRendererProvider.Context，而是直接调用父类 RendererLivingEntity 的构造函数。
     * 使用了一个虚拟的 DummyVanilaModel 来满足父类构造函数的要求，这是Geckolib在旧版本中的常见做法。
     */
    public GeoEntityRenderer(AnimatedGeoModel<T> modelProvider) {
        super(new DummyVanilaModel(), 0);
        this.modelProvider = modelProvider;
        // 将DummyVanilaModel的渲染器指向当前实例，以便进行渲染回调
        if (this.mainModel instanceof DummyVanilaModel) {
            ((DummyVanilaModel) this.mainModel).renderer = this;
        }
    }

    /**
     * 新增方法：从参考代码中添加，用于计算实体的旋转角度。
     * 这是 doRender 逻辑的一部分，被提取出来以便复用和清晰化。
     * @param entity 实体
     * @param partialTicks 渲染间隔帧
     * @param shouldSit 是否坐下
     * @return 一个包含身体旋转角度和头部偏航角度的Pair
     */
    public Pair<Float, Float> calculateRotations(EntityLivingBase entity, float partialTicks, boolean shouldSit) {
        float f = Interpolations.lerpYaw(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
        float f1 = Interpolations.lerpYaw(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
        float netHeadYaw = f1 - f;
        if (shouldSit && entity.ridingEntity instanceof EntityLivingBase) {
            EntityLivingBase livingentity = (EntityLivingBase) entity.ridingEntity;
            f = Interpolations.lerpYaw(livingentity.prevRenderYawOffset, livingentity.renderYawOffset, partialTicks);
            netHeadYaw = f1 - f;
            float f3 = MathHelper.wrapAngleTo180_float(netHeadYaw);
            if (f3 < -85.0F) {
                f3 = -85.0F;
            }
            if (f3 >= 85.0F) {
                f3 = 85.0F;
            }
            f = f1 - f3;
            if (f3 * f3 > 2500.0F) {
                f += f3 * 0.2F;
            }
            netHeadYaw = f1 - f;
        }
        return new ImmutablePair<>(f, netHeadYaw);
    }

    /**
     * 修改：这是核心渲染方法，已完全替换为1.7.10的 `doRender`。
     * 1. 签名从 `render(T, float, float, PoseStack, MultiBufferSource, int)` 改为 `doRender(EntityLivingBase, double, double, double, float, float)`。
     * 2. 渲染逻辑从使用 PoseStack 和 VertexConsumer 改为使用 GlStateManager 和 Tessellator。
     * 3. 动画数据的准备逻辑（如 isSitting, isChild, headPitch, netHeadYaw）被重写以适配旧版实体类。
     * 4. 颜色和光照处理也适配了旧版。
     */
    @Override
    public void doRender(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks) {
        this.animatable = (T) entity;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);

        boolean shouldSit = entity.ridingEntity != null && entity.ridingEntity.shouldRiderSit();
        EntityModelData entityModelData = new EntityModelData();
        entityModelData.isSitting = shouldSit;
        entityModelData.isChild = entity.isChild();

        Pair<Float, Float> rotations = calculateRotations(entity, partialTicks, shouldSit);
        float bodyRot = rotations.getLeft();
        float netHeadYaw = rotations.getRight();
        float headPitch = Interpolations.lerp(entity.prevRotationPitch, entity.rotationPitch, partialTicks);

        float ageInTicks = this.handleRotationFloat(entity, partialTicks);
        this.applyRotations((T) entity, ageInTicks, bodyRot, partialTicks);

        float limbSwingAmount = 0.0F;
        float limbSwing = 0.0F;
        if (!shouldSit && entity.isEntityAlive()) {
            limbSwingAmount = Interpolations.lerp(entity.prevLimbSwingAmount, entity.limbSwingAmount, partialTicks);
            limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
            if (entity.isChild()) {
                limbSwing *= 3.0F;
            }
            if (limbSwingAmount > 1.0F) {
                limbSwingAmount = 1.0F;
            }
        }
        entityModelData.headPitch = -headPitch;
        entityModelData.netHeadYaw = -netHeadYaw;

        AnimationEvent<T> predicate = new AnimationEvent<>((T) entity, limbSwing, limbSwingAmount, partialTicks,
            (limbSwingAmount > -getSwingMotionAnimThreshold() && limbSwingAmount < getSwingMotionAnimThreshold()), Collections.singletonList(entityModelData));

        GeoModel model = modelProvider.getModel(modelProvider.getModelLocation((T) entity));
        this.modelProvider.setCustomAnimations((T) entity, this.getInstanceId((T) entity), predicate);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0.01f, 0); // 避免Z-fighting

        Minecraft.getMinecraft().getTextureManager().bindTexture(getTextureLocation((T) entity));
        Color renderColor = getRenderColor((T) entity, partialTicks); // 调整了 getRenderColor 的调用

        // 在 1.7.10 中，受伤/死亡时的红色效果通常由 setDoRenderBrightness 处理
        boolean flag = this.setDoRenderBrightness((T) entity, partialTicks);

        if (!entity.isInvisible() || (!entity.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer))) {
            // 直接调用 IGeoRenderer 的 render 方法
            render(model, (T) entity, partialTicks,
                (float) renderColor.getRed() / 255f,
                (float) renderColor.getGreen() / 255f,
                (float) renderColor.getBlue() / 255f,
                (float) renderColor.getAlpha() / 255f);
        }

        if (flag) {
            this.unsetDoRenderBrightness();
        }
        GlStateManager.popMatrix();

        // 渲染 Layers
        if (!entity.isPlayerSleeping()) { // 1.7.10 中没有 isSpectator
            for (GeoLayerRenderer<T> layerRenderer : this.layerRenderers) {
                layerRenderer.render((T)entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
            }
        }

        // 渲染拴绳
        if (entity instanceof EntityLiving) {
            Entity leashHolder = ((EntityLiving) entity).getLeashedToEntity();
            if (leashHolder != null) {
                this.renderLeash((EntityLiving) entity, x, y, z, entityYaw, partialTicks);
            }
        }

        GlStateManager.popMatrix();
        // 调用父类的doRender来渲染名字标签等
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    /**
     * 修改：实体唯一ID的获取方式。
     * 高版本使用 `getId()`，旧版本通常使用 `getUniqueID().hashCode()`。
     */
    @Override
    @Keep
    public int getInstanceId(T animatable) {
        return animatable.getUniqueID().hashCode();
    }

    @Override
    @Keep
    public GeoModelProvider<T> getGeoModelProvider() {
        return this.modelProvider;
    }

    @Override
    @Keep
    public ResourceLocation getTextureLocation(T animatable) {
        return this.modelProvider.getTextureLocation(animatable);
    }

    @Override
    @Keep
    public float getWidthScale(T animatable) {
        return this.widthScale;
    }

    @Override
    @Keep
    public float getHeightScale(T entity) {
        return this.heightScale;
    }

    /**
     * 修改：旋转逻辑已替换为1.7.10版本。
     * 使用 `GlStateManager` 替代 `PoseStack` 进行矩阵变换。
     * 移除了对 `Pose` (如 SLEEPING) 和 `isAutoSpinAttack` 的现代逻辑检查，替换为旧版逻辑。
     * Dinnerbone/Grumm 效果的逻辑也已更新。
     */
    protected void applyRotations(T animatable, float ageInTicks, float rotationYaw, float partialTicks) {
        if (!animatable.isPlayerSleeping()) {
            GlStateManager.rotate(180.0F - rotationYaw, 0.0F, 1.0F, 0.0F);
        }

        if (animatable.deathTime > 0) {
            float f = ((float) animatable.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt_float(f);
            if (f > 1.0F) {
                f = 1.0F;
            }
            GlStateManager.rotate(f * this.getDeathMaxRotation(animatable), 0.0F, 0.0F, 1.0F);
        } else if (animatable instanceof EntityLiving && ((EntityLiving) animatable).getCustomNameTag() != null || animatable instanceof EntityPlayer) {
            String s = EnumChatFormatting.getTextWithoutFormattingCodes(animatable.getCommandSenderName());
            if (("Dinnerbone".equals(s) || "Grumm".equalsIgnoreCase(s))) {
                GlStateManager.translate(0.0D, animatable.height + 0.1F, 0.0D);
                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            }
        }
    }

    protected boolean isVisible(T animatable) {
        return !animatable.isInvisible();
    }

    /**
     * 修改：高版本的 getSwingProgress 直接可用。
     * 1.7.10 中对应的方法是 getSwingProgress，这里保持方法名和实现。
     */
    protected float getSwingProgress(T animatable, float partialTick) {
        return animatable.getSwingProgress(partialTick);
    }

    /**
     * 新增方法：从参考代码中添加。
     * 定义了模型动画中 "ageInTicks" 参数的值。
     */
    protected float handleRotationFloat(EntityLivingBase livingBase, float partialTicks) {
        return (float) livingBase.ticksExisted + partialTicks;
    }

    protected float getSwingMotionAnimThreshold() {
        return 0.15f;
    }

    public final boolean addLayer(GeoLayerRenderer<T> layer) {
        return this.layerRenderers.add(layer);
    }

    /**
     * 修改：拴绳的渲染方法已完全替换为1.7.10的版本。
     * 使用 `Tessellator` 手动绘制绳索，替代了高版本中使用 `VertexConsumer` 和 `RenderType.leash()` 的方法。
     * 光照计算逻辑也被移除，因为旧版Tessellator的颜色处理方式不同。
     */
    protected void renderLeash(EntityLiving entityLivingIn, double x, double y, double z, float entityYaw, float partialTicks) {
        Entity entity = entityLivingIn.getLeashedToEntity();

        if (entity != null) {
            y = y - (1.6D - (double) entityLivingIn.height) * 0.5D;
            Tessellator tessellator = Tessellator.instance;
            double d0 = this.interpolateValue(entity.prevRotationYaw, entity.rotationYaw, partialTicks * 0.5F) * 0.01745329238474369D;
            double d1 = this.interpolateValue(entity.prevRotationPitch, entity.rotationPitch, partialTicks * 0.5F) * 0.01745329238474369D;
            double d2 = Math.cos(d0);
            double d3 = Math.sin(d0);
            double d4 = Math.sin(d1);

            if (entity instanceof EntityHanging) {
                d2 = 0.0D;
                d3 = 0.0D;
                d4 = -1.0D;
            }

            double d5 = Math.cos(d1);
            double d6 = this.interpolateValue(entity.prevPosX, entity.posX, partialTicks) - d2 * 0.7D - d3 * 0.5D * d5;
            double d7 = this.interpolateValue(entity.prevPosY + (double) entity.getEyeHeight() * 0.7D, entity.posY + (double) entity.getEyeHeight() * 0.7D, partialTicks) - d4 * 0.5D - 0.25D;
            double d8 = this.interpolateValue(entity.prevPosZ, entity.posZ, partialTicks) - d3 * 0.7D + d2 * 0.5D * d5;
            double d9 = this.interpolateValue(entityLivingIn.prevRenderYawOffset, entityLivingIn.renderYawOffset, partialTicks) * 0.01745329238474369D + (Math.PI / 2D);
            d2 = Math.cos(d9) * (double) entityLivingIn.width * 0.4D;
            d3 = Math.sin(d9) * (double) entityLivingIn.width * 0.4D;
            double d10 = this.interpolateValue(entityLivingIn.prevPosX, entityLivingIn.posX, partialTicks) + d2;
            double d11 = this.interpolateValue(entityLivingIn.prevPosY, entityLivingIn.posY, partialTicks);
            double d12 = this.interpolateValue(entityLivingIn.prevPosZ, entityLivingIn.posZ, partialTicks) + d3;
            x = x + d2;
            //          y = y;  // y is not changed
            z = z + d3;
            double d13 = (double) ((float) (d6 - d10));
            double d14 = (double) ((float) (d7 - d11));
            double d15 = (double) ((float) (d8 - d12));
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();

            // In 1.7.10, Tessellator methods are different
            tessellator.startDrawing(5); // GL_LINE_STRIP might be used. 5 is GL_TRIANGLE_STRIP
            for (int j = 0; j <= 24; ++j) {
                float f = 0.5F;
                float f1 = 0.4F;
                float f2 = 0.3F;
                if (j % 2 == 0) {
                    f *= 0.7F;
                    f1 *= 0.7F;
                    f2 *= 0.7F;
                }
                float f3 = (float) j / 24.0F;
                tessellator.setColorRGBA_F(f, f1, f2, 1.0F);
                tessellator.addVertex(x + d13 * (double) f3 + 0.0D, y + d14 * (double) (f3 * f3 + f3) * 0.5D + (double) ((24.0F - (float) j) / 18.0F + 0.125F), z + d15 * (double) f3);
                tessellator.addVertex(x + d13 * (double) f3 + 0.025D, y + d14 * (double) (f3 * f3 + f3) * 0.5D + (double) ((24.0F - (float) j) / 18.0F + 0.125F) + 0.025D, z + d15 * (double) f3);
            }
            tessellator.draw();

            tessellator.startDrawing(5);
            for (int k = 0; k <= 24; ++k) {
                float f4 = 0.5F;
                float f5 = 0.4F;
                float f6 = 0.3F;
                if (k % 2 == 0) {
                    f4 *= 0.7F;
                    f5 *= 0.7F;
                    f6 *= 0.7F;
                }
                float f7 = (float) k / 24.0F;
                tessellator.setColorRGBA_F(f4, f5, f6, 1.0F);
                tessellator.addVertex(x + d13 * (double) f7 + 0.0D, y + d14 * (double) (f7 * f7 + f7) * 0.5D + (double) ((24.0F - (float) k) / 18.0F + 0.125F) + 0.025D, z + d15 * (double) f7);
                tessellator.addVertex(x + d13 * (double) f7 + 0.025D, y + d14 * (double) (f7 * f7 + f7) * 0.5D + (double) ((24.0F - (float) k) / 18.0F + 0.125F), z + d15 * (double) f7 + 0.025D);
            }
            tessellator.draw();

            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.enableCull();
        }
    }

    /**
     * 新增方法：从参考代码中添加，是 `renderLeash` 的辅助方法。
     * 用于在两点之间进行线性插值。
     */
    private double interpolateValue(double start, double end, double pct) {
        return start + (end - start) * pct;
    }

    /**
     * 新增方法：用于设置受伤/死亡时的颜色叠加效果。
     * 这是1.7.10中处理这类效果的标准方式之一，替代了高版本中的 `getOverlay` 方法。
     * @return 如果颜色被设置，返回true。
     */
    protected boolean setDoRenderBrightness(T entityLivingBaseIn, float partialTicks) {
         return RenderHurtColor.set(entityLivingBaseIn, partialTicks);
    }

    /**
     * 新增方法：取消由 setDoRenderBrightness 设置的颜色效果。
     */
    protected void unsetDoRenderBrightness() {
         RenderHurtColor.unset();
    }


    // ====================================================================================================================================
    // 以下是高版本特有的或已被废弃的方法，已进行相应处理。
    // ====================================================================================================================================

    /**
     * 废弃：此方法是高版本渲染管线的一部分，在 `doRender` 的实现中已被替代。
     * 在1.7.10中，渲染流程从 `doRender` 开始，不需要此方法。
     */
    /*
    @Override
    @Keep
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource,
                            VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue,
                            float partialTicks) { ... }
    */

    /**
     * 废弃：高版本的 `render` 方法已被 `doRender` 替代。
     */
    /*
    @Override
    @Keep
    public void render(T animatable, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) { ... }
    */

    /**
     * 废弃：`shouldShowName` 的逻辑由父类 `RendererLivingEntity` 中的 `passSpecialRender` 方法处理。
     * 在 `doRender` 的末尾调用 `super.doRender()` 即可实现名称标签的渲染。
     */
    /*
    @Override
    @Keep
    public boolean shouldShowName(T animatable) { ... }
    */

    /**
     * 废弃：`getOverlay` 用于获取受伤/死亡时的红色叠加纹理，这是高版本系统。
     * 在1.7.10中，此效果由 `setDoRenderBrightness` 和 `RenderHurtColor` 等机制处理。
     */
    /*
    public int getOverlay(T entity, float u) { ... }
    */

    /**
     * 修改：getFacingAngle 的实现已更新，以使用1.7.10的 `EnumFacing`。
     * 高版本的 `switch` 表达式改为了标准的 `switch` 语句。
     */
    private static float getFacingAngle(EnumFacing facingIn) {
        switch (facingIn) {
            case SOUTH:
                return 90.0F;
            case WEST:
                return 0.0F; // 西面是 0 度
            case NORTH:
                return 270.0F;
            case EAST:
                return 180.0F;
            default:
                return 0.0F;
        }
    }

    // 废弃：以下方法与高版本的渲染周期和 BufferSource 相关，在1.7.10中不再需要。
    /*
    @Override
    @Keep
    @Nonnull
    public IRenderCycle getCurrentModelRenderCycle() { ... }

    @Override
    @Keep
    public void setCurrentModelRenderCycle(IRenderCycle currentModelRenderCycle) { ... }

    @Override
    @Keep
    public MultiBufferSource getCurrentRTB() { ... }

    @Override
    @Keep
    public void setCurrentRTB(MultiBufferSource bufferSource) { ... }
    */
}
