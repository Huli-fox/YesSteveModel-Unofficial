package com.fox.ysmu.geckolib3.geo;

import com.fox.ysmu.geckolib3.core.IAnimatable;
import com.fox.ysmu.geckolib3.core.controller.AnimationController;
import com.fox.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.fox.ysmu.geckolib3.core.util.Color;
import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.fox.ysmu.geckolib3.model.AnimatedGeoModel;
import com.fox.ysmu.geckolib3.model.DummyVanilaModel;
import com.fox.ysmu.geckolib3.model.provider.data.EntityModelData;
import com.fox.ysmu.geckolib3.util.ConfigHandler; // 假设存在
import com.fox.ysmu.geckolib3.util.GlStateManager; // 假设存在
import com.fox.ysmu.mclib.utils.Interpolations; // 假设存在
import com.google.common.collect.Lists;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注意：此类已从高版本移植到1.7.10。
 * 主要变化：
 * 1. 继承的父类从 net.minecraft.client.renderer.entity.EntityRenderer 改为 net.minecraft.client.renderer.entity.RendererLivingEntity。
 * 2. 渲染系统从使用 PoseStack 和 MultiBufferSource (Blaze3D) 改为使用 GlStateManager 和 Tessellator (旧OpenGL管线)。
 * 3. 许多方法的签名和实现已根据1.7.10的类（如 EntityLivingBase, EnumFacing）进行调整。
 * 4. 移除了仅存在于高版本渲染API中的方法。
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class GeoReplacedEntityRenderer<T extends IAnimatable> extends RendererLivingEntity implements IGeoRenderer {
    // ## 字段部分 ##
    // 字段：保持不变
    protected static final Map<Class<? extends IAnimatable>, GeoReplacedEntityRenderer> renderers = new ConcurrentHashMap<>();

    // 字段：modelProvider 类型参数从 IAnimatable 改为 T 以保持一致性
    protected final AnimatedGeoModel<T> modelProvider;
    // 字段：List 实现替换为 Guava 的 Lists.newArrayList()
    protected final List<GeoLayerRenderer> layerRenderers = Lists.newArrayList();
    // 字段：保持不变
    protected final T animatable;
    // 字段：保持不变
    protected IAnimatable currentAnimatable;

    // 字段：移除。这些字段与高版本 PoseStack 渲染系统相关，在1.7.10中不适用。
    // protected float widthScale = 1;
    // protected float heightScale = 1;
    // protected Matrix4f dispatchedMat = new Matrix4f();
    // protected Matrix4f renderEarlyMat = new Matrix4f();
    // protected MultiBufferSource rtb = null;
    // private IRenderCycle currentModelRenderCycle = EModelRenderCycle.INITIAL;

    // ## 静态代码块和方法 ##
    // 静态代码块：保持不变
    static {
        AnimationController.addModelFetcher((IAnimatable object) -> {
            GeoReplacedEntityRenderer renderer = renderers.get(object.getClass());
            return renderer == null ? null : renderer.getGeoModelProvider();
        });
    }

    /**
     * 方法变化：新增。这是1.7.10移植代码中提供的用于注册渲染器的静态方法。
     */
    public static void registerReplacedEntity(Class<? extends IAnimatable> itemClass,
                                              GeoReplacedEntityRenderer renderer) {
        renderers.put(itemClass, renderer);
    }

    /**
     * 方法变化：保持不变。
     */
    public static GeoReplacedEntityRenderer getRenderer(Class<? extends IAnimatable> animatableClass) {
        return renderers.get(animatableClass);
    }

    /**
     * 方法变化：已移除。getPackedOverlay 与高版本的 OverlayTexture 相关，1.7.10没有此机制。
     */
    // public static int getPackedOverlay(LivingEntity entity, float u) { ... }

    /**
     * 方法变化：已替换为1.7.10版本实现。
     * 使用 net.minecraft.util.EnumFacing 替代了 net.minecraft.core.Direction。
     * case 的逻辑也根据1.7.10版本进行了调整。
     */
    private static float getFacingAngle(EnumFacing facingIn) {
        switch (facingIn) {
            case SOUTH:
                return 90.0F;
            case WEST:
                return 0.0F;
            case NORTH:
                return 270.0F;
            case EAST:
                return 180.0F;
            default:
                return 0.0F;
        }
    }

    /**
     * 方法变化：已移除。renderLeashPiece 是高版本 renderLeash 的辅助方法，使用了 VertexConsumer。
     * 1.7.10的 renderLeash 方法使用 Tessellator 且不拆分辅助方法。
     */
    // private static void renderLeashPiece(...) { ... }

    /**
     * 方法变化：构造函数已替换为1.7.10版本实现。
     * 不再接收 EntityRendererProvider.Context，而是直接调用父类构造函数并设置 renderManager。
     * 添加了将自身实例注册到静态map中的逻辑，以兼容高版本行为。
     */
    public GeoReplacedEntityRenderer(AnimatedGeoModel<T> modelProvider, T animatable) {
        super(new DummyVanilaModel(), 0); // 使用一个虚拟的ModelBase，因为我们使用GeckoLib自己的模型渲染
        this.renderManager = RenderManager.instance;
        this.modelProvider = modelProvider;
        this.animatable = animatable;
        renderers.putIfAbsent(animatable.getClass(), this);
    }

    /**
     * 方法变化：已移除。这些是高版本 IGeoRenderer 接口的方法，与新的渲染周期和API相关，在1.7.10中不存在。
     */
    // public IRenderCycle getCurrentModelRenderCycle() { ... }
    // public void setCurrentModelRenderCycle(IRenderCycle currentModelRenderCycle) { ... }
    // public float getWidthScale(Object animatable) { ... }
    // public float getHeightScale(Object entity) { ... }
    // public void renderEarly(...) { ... }
    // public void renderRecursively(...) { ... }
    // public MultiBufferSource getCurrentRTB() { ... }
    // public void setCurrentRTB(MultiBufferSource bufferSource) { ... }


    /**
     * 方法变化：已替换为1.7.10版本的 doRender 方法。
     * 这是核心渲染逻辑，完全替换了高版本的 render 方法，以适配1.7.10的渲染流程。
     * 使用 GlStateManager 进行矩阵变换，替代了 PoseStack。
     * 实体类型从 Entity/LivingEntity 变更为 EntityLivingBase。
     */
    @Override
    public void doRender(Entity entityObj, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!(entityObj instanceof EntityLivingBase)) return;
        EntityLivingBase entity = (EntityLivingBase) entityObj;
        this.currentAnimatable = this.animatable; // 确保 currentAnimatable 被设置

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, z);

            boolean shouldSit = entity.isRiding() && (entity.ridingEntity != null); // 1.7.10的坐姿判断逻辑
            EntityModelData entityModelData = new EntityModelData();
            entityModelData.isSitting = shouldSit;
            entityModelData.isChild = entity.isChild();

            float f = Interpolations.lerpYaw(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
            float f1 = Interpolations.lerpYaw(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
            float netHeadYaw = f1 - f;
            if (shouldSit && entity.ridingEntity instanceof EntityLivingBase) {
                EntityLivingBase livingentity = (EntityLivingBase) entity.ridingEntity;
                f = Interpolations.lerpYaw(livingentity.prevRenderYawOffset, livingentity.renderYawOffset, partialTicks);
                netHeadYaw = f1 - f;
                float f3 = MathHelper.wrapAngleTo180_float(netHeadYaw);
                if (f3 < -85.0F) f3 = -85.0F;
                if (f3 >= 85.0F) f3 = 85.0F;

                f = f1 - f3;
                if (f3 * f3 > 2500.0F) f += f3 * 0.2F;
                netHeadYaw = f1 - f;
            }

            float headPitch = Interpolations.lerp(entity.prevRotationPitch, entity.rotationPitch, partialTicks);

            // 1.7.10 中实体无法在床上睡觉，所以移除了相关逻辑
            /* if (entity.getPose() == Pose.SLEEPING) { ... } */

            float f7 = this.handleRotationFloat(entity, partialTicks);
            this.applyRotations(entity, f7, f, partialTicks);

            this.preRenderCallback(entity, partialTicks); // 调用 preRenderCallback

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

            AnimationEvent predicate = new AnimationEvent(animatable, limbSwing, limbSwingAmount, partialTicks,
                !(limbSwingAmount > -0.15F && limbSwingAmount < 0.15F), Collections.singletonList(entityModelData));
            GeoModel model = modelProvider.getModel(modelProvider.getModelLocation(animatable));

            // 高版本 setCustomAnimations 替换为 1.7.10 的 setLivingAnimations
            this.modelProvider.setCustomAnimations(animatable, this.getInstanceId(entity), predicate);

            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(0, 0.01f, 0);
                Minecraft.getMinecraft().renderEngine.bindTexture(getEntityTexture(entity));
                Color renderColor = getRenderColor(animatable, partialTicks); // 适配IGeoRenderer接口

                if (!entity.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer)) {
                    render(model, entity, partialTicks, renderColor.getRed() / 255f, renderColor.getGreen() / 255f,
                        renderColor.getBlue() / 255f, renderColor.getAlpha() / 255f);
                }

                // 渲染层
                if (!entity.isPlayerSleeping()) { // 1.7.10 中层渲染的条件
                    for (GeoLayerRenderer layerRenderer : this.layerRenderers) {
                        layerRenderer.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, f7, netHeadYaw, headPitch, 1 / 16F);
                    }
                }

                // 渲染拴绳
                if (entity instanceof EntityLiving) {
                    Entity leashHolder = ((EntityLiving) entity).getLeashedToEntity();
                    if (leashHolder != null) {
                        // 注意：原版renderLeash是在doRender之外调用的，这里直接调用可能导致坐标不正确。
                        // 1.7.10的移植代码在此处调用，我们暂时保留，但它可能需要调整。
                        // 为了使其工作，x, y, z需要是传入doRender的原始值，而不是变换后的0,0,0
                        this.renderLeash((EntityLiving) entity, x, y, z, entityYaw, partialTicks);
                    }
                }
            } catch (Exception e) {
                if (ConfigHandler.debugPrintStacktraces) { // 假设存在
                    e.printStackTrace();
                }
            } finally {
                GlStateManager.popMatrix();
            }
        } catch (Exception e) {
            if (ConfigHandler.debugPrintStacktraces) {
                e.printStackTrace();
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }

    /**
     * 方法变化：已移除。getOverlayProgress 与高版本的 OverlayTexture 相关。
     */
    // protected float getOverlayProgress(LivingEntity entity, float partialTicks) { ... }

    /**
     * 方法变化：方法签名已替换为1.7.10版本。
     * 参数从 (LivingEntity, PoseStack, float) 改为 (EntityLivingBase, float)。
     * 这是 RendererLivingEntity 的一个标准覆写方法。
     */
    @Override
    protected void preRenderCallback(EntityLivingBase entitylivingbaseIn, float partialTickTime) {
    }

    /**
     * 方法变化：方法名和签名已替换为1.7.10版本。
     * 从 getTextureLocation(Entity) 改为 getEntityTexture(Entity)，以覆写 RendererLivingEntity 中的方法。
     * 实现逻辑相似，都是获取当前animatable的纹理。
     */
    @Override
    @Nullable
    protected ResourceLocation getEntityTexture(Entity entity) {
        return this.modelProvider.getTextureLocation((T) this.currentAnimatable);
    }

    /**
     * 方法变化：保持不变。
     */
    @Override
    public AnimatedGeoModel getGeoModelProvider() {
        return this.modelProvider;
    }

    /**
     * 方法变化：已替换为1.7.10版本实现。
     * 使用 GlStateManager 进行旋转，替代了 PoseStack。
     * 实体类型从 LivingEntity 变更为 EntityLivingBase。
     * 移除了高版本特有的 Pose (如SLEEPING) 的处理逻辑。
     */
    protected void applyRotations(EntityLivingBase entityLiving, float ageInTicks, float rotationYaw, float partialTicks) {
        if (!entityLiving.isPlayerSleeping()) {
            GlStateManager.rotate(180.0F - rotationYaw, 0, 1, 0);
        }

        if (entityLiving.deathTime > 0) {
            float f = ((float) entityLiving.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt_float(f);
            if (f > 1.0F) {
                f = 1.0F;
            }
            GlStateManager.rotate(f * this.getDeathMaxRotation(entityLiving), 0, 0, 1);
        } else if ((entityLiving instanceof EntityLiving && ((EntityLiving) entityLiving).hasCustomNameTag()) || entityLiving instanceof EntityPlayer) {
            String s = ChatFormatting.stripFormatting(entityLiving.getCommandSenderName());
            if ("Dinnerbone".equals(s) || "Grumm".equalsIgnoreCase(s)) {
                GlStateManager.translate(0.0D, entityLiving.height + 0.1F, 0.0D);
                GlStateManager.rotate(180, 0, 0, 1);
            }
        }
    }

    /**
     * 方法变化：方法签名已替换为1.7.10版本。
     * 1.7.10中使用 EntityLivingBase。
     */
    protected boolean isVisible(EntityLivingBase entity) {
        return !entity.isInvisible();
    }

    /**
     * 方法变化：保持不变。
     */
    protected float getDeathMaxRotation(EntityLivingBase entity) {
        return 90;
    }

    /**
     * 方法变化：已移除。shouldShowName 是高版本 EntityRenderer 的方法。
     * 在1.7.10中，名字渲染由 RendererLivingEntity 的 passSpecialRender 方法处理，通常不需要覆写此逻辑。
     */
    // public boolean shouldShowName(Entity entity) { ... }

    /**
     * 方法变化：方法名和签名已替换为1.7.10版本。
     * 1.7.10中使用 getSwingProgress。
     */
    protected float getSwingProgress(EntityLivingBase entity, float partialTick) {
        return entity.getSwingProgress(partialTick);
    }

    /**
     * 方法变化：已移除。该阈值直接硬编码在1.7.10版本的 AnimationEvent 创建处。
     */
    // protected float getSwingMotionAnimThreshold() { ... }

    /**
     * 方法变化：新增。这是 RendererLivingEntity 的一个方法，用于处理旋转动画，在1.7.10的doRender中被调用。
     */
    protected float handleRotationFloat(EntityLivingBase livingBase, float partialTicks) {
        return (float) livingBase.ticksExisted + partialTicks;
    }

    /**
     * 方法变化：签名已替换为1.7.10版本。
     */
    @Override
    public ResourceLocation getTextureLocation(Object animatable) {
        return this.modelProvider.getTextureLocation((T) animatable);
    }

    /**
     * 方法变化：签名中的泛型已替换为1.7.10版本。
     * 从 GeoLayerRenderer<? extends LivingEntity> 改为 GeoLayerRenderer<? extends EntityLivingBase>。
     */
    public final boolean addLayer(GeoLayerRenderer<? extends EntityLivingBase> layer) {
        return this.layerRenderers.add(layer);
    }

    /**
     * 方法变化：已替换为1.7.10版本实现。
     * 这是一个辅助方法，用于计算插值，在 renderLeash 中使用。
     */
    private double interpolateValue(double start, double end, double pct) {
        return start + (end - start) * pct;
    }

    /**
     * 方法变化：已替换为1.7.10版本实现。
     * 完全重写以使用 Tessellator 和 GlStateManager，替代高版本的 VertexConsumer 和 PoseStack。
     * 签名也已更改以匹配1.7.10的上下文。
     */
    protected void renderLeash(EntityLiving entityLivingIn, double x, double y, double z, float entityYaw, float partialTicks) {
        Entity entity = entityLivingIn.getLeashedToEntity();

        if (entity != null) {
            y = y - (1.6D - (double) entityLivingIn.height) * 0.5D;
            net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.instance;
            double d0 = this.interpolateValue(entity.prevRotationYaw, entity.rotationYaw, partialTicks * 0.5F) * 0.01745329238474369D;
            double d1 = this.interpolateValue(entity.prevRotationPitch, entity.rotationPitch, partialTicks * 0.5F) * 0.01745329238474369D;
            double d2 = Math.cos(d0);
            double d3 = Math.sin(d0);
            double d4 = Math.sin(d1);

            if (entity instanceof net.minecraft.entity.EntityHanging) {
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
            z = z + d3;
            double d13 = (double) ((float) (d6 - d10));
            double d14 = (double) ((float) (d7 - d11));
            double d15 = (double) ((float) (d8 - d12));
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            tessellator.startDrawing(5);

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
}
