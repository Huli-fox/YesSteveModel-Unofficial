// TODO ETFU未提供替换鞘翅渲染的方法
//package com.fox.ysmu.client.renderer.layer;
//
//import software.bernie.geckolib3.core.IAnimatable;
//import software.bernie.geckolib3.geo.GeoLayerRenderer;
//import software.bernie.geckolib3.geo.IGeoRenderer;
//import software.bernie.geckolib3.geo.render.built.GeoBone;
//import software.bernie.geckolib3.geo.render.built.GeoModel;
//import software.bernie.geckolib3.util.RenderUtils;
//import com.mojang.blaze3d.vertex.PoseStack;
//import com.mojang.blaze3d.vertex.VertexConsumer;
//import com.fox.ysmu.compat.Axis;
//import net.minecraft.client.model.ElytraModel;
//import net.minecraft.client.model.geom.EntityModelSet;
//import net.minecraft.client.model.geom.ModelLayers;
//import net.minecraft.client.player.AbstractClientPlayer;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.RenderType;
//import net.minecraft.client.renderer.entity.ItemRenderer;
//import net.minecraft.client.renderer.texture.OverlayTexture;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.entity.EquipmentSlot;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.entity.player.PlayerModelPart;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.Items;
//
//public class CustomPlayerElytraLayer<T extends LivingEntity & IAnimatable> extends GeoLayerRenderer<T> {
//    private static final ResourceLocation WINGS_LOCATION = new ResourceLocation("textures/entity/elytra.png");
//    private final ElytraModel<T> elytraModel;
//
//    public CustomPlayerElytraLayer(IGeoRenderer<T> entityRendererIn, EntityModelSet modelSet) {
//        super(entityRendererIn);
//        this.elytraModel = new ElytraModel<>(modelSet.bakeLayer(ModelLayers.ELYTRA));
//    }
//
//    @Override
//    public void render(PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn, T livingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
//        ItemStack stack = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
//        if (stack.getItem() == Items.ELYTRA && this.entityRenderer.getGeoModel() != null) {
//            GeoModel geoModel = this.entityRenderer.getGeoModel();
//            if (!geoModel.elytraBones.isEmpty()) {
//                ResourceLocation texture;
//                if (livingEntity instanceof AbstractClientPlayer player) {
//                    if (player.isElytraLoaded() && player.getElytraTextureLocation() != null) {
//                        texture = player.getElytraTextureLocation();
//                    } else if (player.isCapeLoaded() && player.getCloakTextureLocation() != null && player.isModelPartShown(PlayerModelPart.CAPE)) {
//                        texture = player.getCloakTextureLocation();
//                    } else {
//                        texture = WINGS_LOCATION;
//                    }
//                } else {
//                    texture = WINGS_LOCATION;
//                }
//                poseStack.pushPose();
//                translateToElytra(poseStack, geoModel);
//                poseStack.translate(0, 1.5, 0);
//                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
//                poseStack.scale(2.0f, 2.0f, 2.0f);
//                this.elytraModel.setupAnim(livingEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
//                VertexConsumer vertexconsumer = ItemRenderer.getArmorFoilBuffer(bufferIn, RenderType.armorCutoutNoCull(texture), false, stack.hasFoil());
//                this.elytraModel.renderToBuffer(poseStack, vertexconsumer, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
//                poseStack.popPose();
//            }
//        }
//    }
//
//    protected void translateToElytra(GeoModel geoModel) {
//        int size = geoModel.elytraBones.size();
//        for (int i = 0; i < size - 1; i++) {
//            RenderUtils.prepMatrixForBone(geoModel.elytraBones.get(i));
//        }
//        GeoBone lastBone = geoModel.elytraBones.get(size - 1);
//        RenderUtils.translateMatrixToBone(lastBone);
//        RenderUtils.translateToPivotPoint(lastBone);
//        RenderUtils.rotateMatrixAroundBone(lastBone);
//        RenderUtils.scaleMatrixForBone(lastBone);
//    }
//}
