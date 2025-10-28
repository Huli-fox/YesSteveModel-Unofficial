package com.fox.ysmu.util;

import com.fox.ysmu.client.entity.CustomPlayerEntity;
import com.fox.ysmu.client.event.RegisterEntityRenderersEvent;
import com.fox.ysmu.client.renderer.CustomPlayerRenderer;
import com.fox.ysmu.geckolib3.core.IAnimatable;
import com.fox.ysmu.geckolib3.geo.GeoReplacedEntityRenderer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.joml.Quaternionf;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@SuppressWarnings("all")
public final class RenderUtil {
    public static void renderTextureScreenEntity(float pPosX, float pPosY, float pScale, float pitch, float yaw, Player player, ResourceLocation modelId, ResourceLocation textureId, boolean showGround, Consumer<CustomPlayerEntity> consumer) {
        if (player == null) {
            return;
        }
        try {
            CustomPlayerRenderer renderer = RegisterEntityRenderersEvent.getInstance();
            IAnimatable animatable = AnimatableCacheUtil.TEXTURE_GUI_CACHE.get(modelId, CustomPlayerEntity::new);
            if (animatable instanceof CustomPlayerEntity entity) {
                consumer.accept(entity);

                entity.setMainModel(ModelIdUtil.getMainId(modelId));
                entity.setTexture(textureId);

                PoseStack viewStack = RenderSystem.getModelViewStack();
                viewStack.pushPose();
                viewStack.translate(pPosX, pPosY, 1050.0D);
                viewStack.scale(1.0F, 1.0F, -1.0F);
                RenderSystem.applyModelViewMatrix();

                PoseStack poseStack = new PoseStack();
                poseStack.translate(0.0D, 0.0D, 1000.0D);
                poseStack.scale(pScale, pScale, pScale);
                poseStack.translate(0, 0.8, 0);
                Quaternionf zp = Axis.ZP.rotationDegrees(180.0F);
                Quaternionf xp = Axis.XP.rotationDegrees(-10 + pitch);
                zp.mul(xp);
                poseStack.mulPose(zp);

                float yBodyRot = player.yBodyRot;
                float yRot = player.getYRot();
                float xRot = player.getXRot();
                float yHeadRotO = player.yHeadRotO;
                float yHeadRot = player.yHeadRot;
                Pose pose = player.getPose();

                player.yBodyRot = -yaw;
                player.setYRot(180);
                player.setXRot(0);
                player.yHeadRot = player.getYRot();
                player.yHeadRotO = player.getYRot();

                Lighting.setupForEntityInInventory();
                EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
                xp.conjugate();
                dispatcher.overrideCameraOrientation(xp);
                dispatcher.setRenderShadow(false);
                MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
                RenderSystem.runAsFancy(() -> {
                    if (entity.hasPreviewAnimation("sleep")) {
                        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90));
                        poseStack.translate(0.5, 0.5625, 0);
                        player.setPose(Pose.SLEEPING);
                    }
                    if (entity.hasPreviewAnimation("swim") || entity.hasPreviewAnimation("swim_stand")) {
                        player.setPose(Pose.SWIMMING);
                    }
                    if (entity.hasPreviewAnimation("sneak") || entity.hasPreviewAnimation("sneaking")) {
                        player.setPose(Pose.CROUCHING);
                    }
                    if (entity.hasPreviewAnimation("sit")) {
                        poseStack.translate(0, -0.5, 0);
                    }
                    if (entity.hasPreviewAnimation("ride")) {
                        poseStack.translate(0, 0.85, 0);
                    }
                    if (entity.hasPreviewAnimation("ride_pig")) {
                        poseStack.translate(0, 0.3125, 0);
                    }
                    if (entity.hasPreviewAnimation("boat")) {
                        poseStack.translate(0, -0.45, 0);
                    }
                    renderer.render(player, animatable, 0, 1.0f, poseStack, bufferSource, 0xf000f0);
                    try {
                        renderExtraEntity(yaw, player, entity, poseStack, dispatcher, bufferSource);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                    if (showGround) {
                        if (entity.hasPreviewAnimation("sleep")) {
                            renderBed(pScale, pitch, yaw, bufferSource);
                        }
                        renderGround(pScale, pitch, yaw, bufferSource);
                    }
                });
                bufferSource.endBatch();
                dispatcher.setRenderShadow(true);

                player.yBodyRot = yBodyRot;
                player.setYRot(yRot);
                player.setXRot(xRot);
                player.yHeadRotO = yHeadRotO;
                player.yHeadRot = yHeadRot;
                player.setPose(pose);

                viewStack.popPose();
                RenderSystem.applyModelViewMatrix();
                Lighting.setupFor3DItems();
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static void renderBed(float scale, float pitch, float yaw, MultiBufferSource.BufferSource bufferSource) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0D, 0.0D, 1000.0D);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, 0.8, 0);
        Quaternionf zp = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf xp = Axis.XP.rotationDegrees(-10 + pitch);
        zp.mul(xp);
        poseStack.mulPose(zp);

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180));
        poseStack.translate(-0.5, 0, 0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.RED_BED.defaultBlockState(), poseStack, bufferSource, 0xf000f0, OverlayTexture.NO_OVERLAY);
    }

    private static void renderGround(float scale, float pitch, float yaw, MultiBufferSource.BufferSource bufferSource) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0D, 0.0D, 1000.0D);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, 0.8, 0);
        Quaternionf zp = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf xp = Axis.XP.rotationDegrees(-10 + pitch);
        zp.mul(xp);
        poseStack.mulPose(zp);

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-1.5, -1, -2.5);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                poseStack.translate(0, 0, 1);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.GRASS_BLOCK.defaultBlockState(), poseStack, bufferSource, 0xf000f0, OverlayTexture.NO_OVERLAY);
            }
            poseStack.translate(1, 0, -3);
        }
        poseStack.translate(-1, 1, 1);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.GRASS.defaultBlockState(), poseStack, bufferSource, 0xf000f0, OverlayTexture.NO_OVERLAY);
        poseStack.translate(0, 0, 1);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.RED_TULIP.defaultBlockState(), poseStack, bufferSource, 0xf000f0, OverlayTexture.NO_OVERLAY);

    }

    private static void renderExtraEntity(float yaw, Player player, CustomPlayerEntity playerEntity, PoseStack poseStack, EntityRenderDispatcher dispatcher, MultiBufferSource.BufferSource bufferSource) throws ExecutionException {
        if (playerEntity.hasPreviewAnimation("ride")) {
            Entity entity = AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.HORSE), () -> EntityType.HORSE.create(player.level()));
            renderExtraEntity(yaw, player, poseStack, dispatcher, bufferSource, entity);
            return;
        }
        if (playerEntity.hasPreviewAnimation("ride_pig")) {
            Entity entity = AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.PIG), () -> EntityType.PIG.create(player.level()));
            renderExtraEntity(yaw, player, poseStack, dispatcher, bufferSource, entity);
            return;
        }
        if (playerEntity.hasPreviewAnimation("boat")) {
            Entity entity = AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.BOAT), () -> EntityType.BOAT.create(player.level()));
            renderExtraEntity(yaw, player, poseStack, dispatcher, bufferSource, entity);
            return;
        }
    }

    private static void renderExtraEntity(float yaw, Player player, PoseStack poseStack, EntityRenderDispatcher dispatcher, MultiBufferSource.BufferSource bufferSource, Entity entity) {
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        dispatcher.render(entity, 0, -entity.getPassengersRidingOffset() - player.getMyRidingOffset(), 0, 0, 1.0f, poseStack, bufferSource, 0xf000f0);
    }

    public static void renderEntityInInventory(int pPosX, int pPosY, int pScale, Player player, ResourceLocation modelId, ResourceLocation textureId, Consumer<CustomPlayerEntity> consumer) {
        if (player == null) {
            return;
        }
        try {
            CustomPlayerRenderer renderer = RegisterEntityRenderersEvent.getInstance();
            IAnimatable animatable = AnimatableCacheUtil.ANIMATABLE_CACHE.get(modelId, CustomPlayerEntity::new);
            if (animatable instanceof CustomPlayerEntity entity) {
                consumer.accept(entity);
                renderModel((double) pPosX, (double) pPosY, (float) pScale, player, modelId, textureId, renderer, entity);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void renderEntityInInventory(int pPosX, int pPosY, int pScale, Player player, ResourceLocation modelId, ResourceLocation textureId) {
        renderEntityInInventory(pPosX, pPosY, pScale, player, modelId, textureId, entity -> {
            if (entity.hasPreviewAnimation()) {
                entity.clearPreviewAnimation();
            }
        });
    }

    private static void renderModel(double pPosX, double pPosY, float pScale, Player player, ResourceLocation modelId, ResourceLocation textureId, GeoReplacedEntityRenderer renderer, CustomPlayerEntity entity) {
        entity.setMainModel(ModelIdUtil.getMainId(modelId));
        entity.setTexture(textureId);

        PoseStack viewStack = RenderSystem.getModelViewStack();
        viewStack.pushPose();
        viewStack.translate(pPosX, pPosY, 1050.0D);
        viewStack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();

        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0D, 0.0D, 1000.0D);
        poseStack.scale(pScale, pScale, pScale);
        Quaternionf zp = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf xp = Axis.XP.rotationDegrees(-10);
        zp.mul(xp);
        poseStack.mulPose(zp);

        float yBodyRot = player.yBodyRot;
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float yHeadRotO = player.yHeadRotO;
        float yHeadRot = player.yHeadRot;

        ItemStack[] itemStacks = new ItemStack[EquipmentSlot.values().length];
        int i = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            itemStacks[i] = player.getItemBySlot(slot);
            if (slot == EquipmentSlot.MAINHAND) {
                player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
            } else if (slot == EquipmentSlot.OFFHAND) {
                player.getInventory().offhand.set(0, ItemStack.EMPTY);
            } else {
                player.getInventory().armor.set(slot.getIndex(), ItemStack.EMPTY);
            }
            i++;
        }

        player.yBodyRot = 200;
        player.setYRot(180);
        player.setXRot(0);
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        xp.conjugate();
        dispatcher.overrideCameraOrientation(xp);
        dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> {
            renderer.render(player, entity, 0, 1.0f, poseStack, bufferSource, 0xf000f0);
        });
        bufferSource.endBatch();
        dispatcher.setRenderShadow(true);

        player.yBodyRot = yBodyRot;
        player.setYRot(yRot);
        player.setXRot(xRot);
        player.yHeadRotO = yHeadRotO;
        player.yHeadRot = yHeadRot;

        i = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack itemStack = itemStacks[i];
            if (slot == EquipmentSlot.MAINHAND) {
                player.getInventory().items.set(player.getInventory().selected, itemStack);
            } else if (slot == EquipmentSlot.OFFHAND) {
                player.getInventory().offhand.set(0, itemStack);
            } else {
                player.getInventory().armor.set(slot.getIndex(), itemStack);
            }
            i++;
        }

        viewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }

    public static void renderPlayerEntity(Player player, double posX, double posY, float scale, float yawOffset, int z) {
        PoseStack viewStack = RenderSystem.getModelViewStack();
        viewStack.pushPose();
        viewStack.translate(posX + scale * 0.5, posY + scale * 2, z);
        viewStack.scale(1, 1, -1);
        RenderSystem.applyModelViewMatrix();
        PoseStack stack = new PoseStack();
        stack.scale(scale, scale, scale);
        Quaternionf zRot = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf yRot = Axis.YP.rotationDegrees(player.yBodyRot + yawOffset - 180);
        zRot.mul(yRot);
        stack.mulPose(zRot);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher renderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        yRot.conjugate();
        renderDispatcher.overrideCameraOrientation(yRot);
        renderDispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> renderDispatcher.render(player, 0, 0, 0.0D, 0.0F, 1.0F, stack, buffer, 15728880));
        buffer.endBatch();
        renderDispatcher.setRenderShadow(true);
        viewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }
}
