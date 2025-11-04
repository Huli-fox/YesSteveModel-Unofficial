// package com.fox.ysmu.util;
//
// import java.util.concurrent.ExecutionException;
// import java.util.function.Consumer;
//
// import net.geckominecraft.client.renderer.GlStateManager;
// import net.minecraft.client.renderer.RenderBlocks;
// import net.minecraft.client.renderer.RenderHelper;
// import net.minecraft.client.renderer.entity.RenderManager;
// import net.minecraft.entity.Entity;
// import net.minecraft.entity.player.EntityPlayer;
// import net.minecraft.init.Blocks;
// import net.minecraft.item.ItemStack;
// import net.minecraft.util.ResourceLocation;
//
// import org.joml.Quaternionf;
// import org.lwjgl.opengl.GL11;
// import org.lwjgl.util.vector.Quaternion;
//
// import com.fox.ysmu.client.ClientEventHandler;
// import com.fox.ysmu.client.entity.CustomPlayerEntity;
// import com.fox.ysmu.client.renderer.CustomPlayerRenderer;
// import com.fox.ysmu.compat.Axis;
// import com.fox.ysmu.compat.BackhandCompat;
// import com.fox.ysmu.compat.Utils;
//
// import software.bernie.geckolib3.core.IAnimatable;
// import software.bernie.geckolib3.geo.GeoReplacedEntityRenderer;
//
// @SuppressWarnings("all")
// public final class RenderUtil {
//
// public static void renderTextureScreenEntity(float pPosX, float pPosY, float pScale, float pitch, float yaw,
// EntityPlayer player, ResourceLocation modelId, ResourceLocation textureId, boolean showGround,
// Consumer<CustomPlayerEntity> consumer) {
// if (player == null) {
// return;
// }
// try {
// CustomPlayerRenderer renderer = ClientEventHandler.getInstance();
// IAnimatable animatable = AnimatableCacheUtil.TEXTURE_GUI_CACHE.get(modelId, CustomPlayerEntity::new);
// if (animatable instanceof CustomPlayerEntity entity) {
// consumer.accept(entity);
//
// entity.setMainModel(ModelIdUtil.getMainId(modelId));
// entity.setTexture(textureId);
//
// GlStateManager.pushMatrix();
// GlStateManager.matrixMode(GL11.GL_MODELVIEW);
// GlStateManager.translate(pPosX, pPosY, 1050.0D);
// GlStateManager.scale(1.0F, 1.0F, -1.0F);
//
// GlStateManager.pushMatrix();
// GlStateManager.translate(0.0D, 0.0D, 1000.0D);
// GlStateManager.scale(pScale, pScale, pScale);
// GlStateManager.translate(0, 0.8, 0);
// Quaternionf zp = Axis.ZP.rotationDegrees(180.0F);
// Quaternionf xp = Axis.XP.rotationDegrees(-10 + pitch);
// zp.mul(xp);
// GlStateManager.rotate(j2l(zp)); // poseStack.mulPose
//
// // 保存玩家原始状态
// float yBodyRot = player.renderYawOffset;
// float yRot = player.rotationYaw;
// float xRot = player.rotationPitch;
// float yHeadRotO = player.prevRotationYawHead;
// float yHeadRot = player.rotationYawHead;
// Pose pose = player.getPose();
//
// // 修改玩家状态用于渲染
// player.renderYawOffset = -yaw;
// player.rotationYaw = 180; // setYRot
// player.rotationPitch = 0; // setXRot
// player.rotationYawHead = player.rotationYaw;
// player.prevRotationYawHead = player.rotationYaw;
//
// RenderHelper.enableGUIStandardItemLighting();
// RenderManager dispatcher = RenderManager.instance;
//
// xp.conjugate();
// dispatcher.overrideCameraOrientation(xp);
// dispatcher.setRenderShadow(false);
//
// GlStateManager.pushMatrix();
// if (entity.hasPreviewAnimation("sleep")) {
// GlStateManager.rotate(j2l(Axis.YP.rotationDegrees(yaw - 90)));
// GlStateManager.translate(0.5, 0.5625, 0);
// // TODO sleep和sneak要处理下
// // player.setPose(Pose.SLEEPING);
// }
// if (entity.hasPreviewAnimation("swim") || entity.hasPreviewAnimation("swim_stand")) {
// // player.setPose(Pose.SWIMMING);
// }
// if (entity.hasPreviewAnimation("sneak") || entity.hasPreviewAnimation("sneaking")) {
// // player.setPose(Pose.CROUCHING);
// }
// if (entity.hasPreviewAnimation("sit")) {
// GlStateManager.translate(0, -0.5, 0);
// }
// if (entity.hasPreviewAnimation("ride")) {
// GlStateManager.translate(0, 0.85, 0);
// }
// if (entity.hasPreviewAnimation("ride_pig")) {
// GlStateManager.translate(0, 0.3125, 0);
// }
// if (entity.hasPreviewAnimation("boat")) {
// GlStateManager.translate(0, -0.45, 0);
// }
// // renderer.doRender();
// try {
// renderExtraEntity(yaw, player, entity, dispatcher);
// } catch (ExecutionException e) {
// throw new RuntimeException(e);
// }
// GlStateManager.popMatrix(); // 弹出动画位移矩阵
// if (showGround) {
// if (entity.hasPreviewAnimation("sleep")) {
// renderBed(pScale, pitch, yaw);
// }
// renderGround(pScale, pitch, yaw);
// }
//
// // 恢复玩家状态
// player.renderYawOffset = yBodyRot;
// player.rotationYaw = yRot;
// player.rotationPitch = xRot;
// player.prevRotationYawHead = yHeadRotO;
// player.rotationYawHead = yHeadRot;
// // player.setPose(pose);
//
// GlStateManager.popMatrix(); // 弹出模型变换矩阵
// GlStateManager.popMatrix(); // 弹出视图变换矩阵
// // 替换 Lighting.setupFor3DItems();
// RenderHelper.enableStandardItemLighting();
// }
// } catch (ExecutionException e) {
// e.printStackTrace();
// }
// }
//
// // 创建一个全局的RenderBlocks实例以提高效率
// private static final RenderBlocks renderBlocks = new RenderBlocks();
//
// private static void renderBed(float scale, float pitch, float yaw) {
// GlStateManager.pushMatrix();
// GlStateManager.translate(0.0D, 0.0D, 1000.0D);
// GlStateManager.scale(scale, scale, scale);
// GlStateManager.translate(0, 0.8, 0);
// Quaternionf zp = Axis.ZP.rotationDegrees(180.0F);
// Quaternionf xp = Axis.XP.rotationDegrees(-10 + pitch);
// zp.mul(xp);
// GlStateManager.rotate(j2l(zp));
//
// GlStateManager.rotate(j2l(Axis.YP.rotationDegrees(yaw + 180)));
// GlStateManager.translate(-0.5, 0, 0.5);
// // Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.RED_BED.defaultBlockState(), poseStack,
// // bufferSource, 0xf000f0, OverlayTexture.NO_OVERLAY);
// RenderManager.instance.renderEngine.bindTexture(new ResourceLocation("textures/entity/bed/red.png"));
// renderBlocks.renderBlockAsItem(Blocks.bed, 0, 1.0F);
// GlStateManager.popMatrix();
// }
//
// private static void renderGround(float scale, float pitch, float yaw) {
// GlStateManager.pushMatrix();
// GlStateManager.translate(0.0D, 0.0D, 1000.0D);
// GlStateManager.scale(scale, scale, scale);
// GlStateManager.translate(0, 0.8, 0);
// Quaternionf zp = Axis.ZP.rotationDegrees(180.0F);
// Quaternionf xp = Axis.XP.rotationDegrees(-10 + pitch);
// zp.mul(xp);
// GlStateManager.rotate(j2l(zp));
//
// GlStateManager.rotate(j2l(Axis.YP.rotationDegrees(yaw)));
// GlStateManager.translate(-1.5, -1, -2.5);
// RenderManager.instance.renderEngine.bindTexture(new ResourceLocation("textures/atlas/blocks.png"));
// for (int i = 0; i < 3; i++) {
// for (int j = 0; j < 3; j++) {
// GlStateManager.pushMatrix();
// GlStateManager.translate(0, 0, 1);
// renderBlocks.renderBlockAsItem(Blocks.grass, 0, 1.0F);
// GlStateManager.popMatrix();
// }
// GlStateManager.translate(1, 0, -3);
// }
// GlStateManager.pushMatrix();
// GlStateManager.translate(-1, 1, 1);
// renderBlocks.renderBlockAsItem(Blocks.tallgrass, 1, 1.0F); // metadata 1 for grass
// GlStateManager.popMatrix();
//
// GlStateManager.pushMatrix();
// GlStateManager.translate(0, 0, 1);
// renderBlocks.renderBlockAsItem(Blocks.red_flower, 0, 1.0F); // metadata 0 for poppy (red tulip)
// GlStateManager.popMatrix();
//
// GlStateManager.popMatrix();
// }
//
// private static void renderExtraEntity(float yaw, EntityPlayer player, CustomPlayerEntity playerEntity,
// RenderManager dispatcher) throws ExecutionException {
// if (playerEntity.hasPreviewAnimation("ride")) {
// // Entity entity = AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.HORSE), () ->
// // EntityType.HORSE.create(player.level()));
// // renderExtraEntity(yaw, player, dispatcher, entity);
// return;
// }
// if (playerEntity.hasPreviewAnimation("ride_pig")) {
// // Entity entity = AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.PIG), () ->
// // EntityType.PIG.create(player.level()));
// // renderExtraEntity(yaw, player, dispatcher, entity);
// return;
// }
// if (playerEntity.hasPreviewAnimation("boat")) {
// // Entity entity = AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.BOAT), () ->
// // EntityType.BOAT.create(player.level()));
// // renderExtraEntity(yaw, player, dispatcher, entity);
// return;
// }
// }
//
// private static void renderExtraEntity(float yaw, EntityPlayer player, RenderManager dispatcher, Entity entity) {
// GlStateManager.pushMatrix();
// GlStateManager.rotate(j2l(Axis.YP.rotationDegrees(yaw)));
// double yOffset = -entity.getMountedYOffset();
// dispatcher.renderEntityWithPosYaw(entity, 0, yOffset, 0, 0, 1.0f);
// GlStateManager.popMatrix();
// }
//
// public static void renderEntityInInventory(int pPosX, int pPosY, int pScale, EntityPlayer player,
// ResourceLocation modelId, ResourceLocation textureId, Consumer<CustomPlayerEntity> consumer) {
// if (player == null) {
// return;
// }
// try {
// CustomPlayerRenderer renderer = ClientEventHandler.getInstance();
// IAnimatable animatable = AnimatableCacheUtil.ANIMATABLE_CACHE.get(modelId, CustomPlayerEntity::new);
// if (animatable instanceof CustomPlayerEntity entity) {
// consumer.accept(entity);
// renderModel(
// (double) pPosX,
// (double) pPosY,
// (float) pScale,
// player,
// modelId,
// textureId,
// renderer,
// entity);
// }
// } catch (ExecutionException e) {
// e.printStackTrace();
// }
// }
//
// public static void renderEntityInInventory(int pPosX, int pPosY, int pScale, EntityPlayer player,
// ResourceLocation modelId, ResourceLocation textureId) {
// renderEntityInInventory(pPosX, pPosY, pScale, player, modelId, textureId, entity -> {
// if (entity.hasPreviewAnimation()) {
// entity.clearPreviewAnimation();
// }
// });
// }
//
// private static void renderModel(double pPosX, double pPosY, float pScale, EntityPlayer player,
// ResourceLocation modelId, ResourceLocation textureId, GeoReplacedEntityRenderer renderer,
// CustomPlayerEntity entity) {
// entity.setMainModel(ModelIdUtil.getMainId(modelId));
// entity.setTexture(textureId);
//
// GlStateManager.pushMatrix();
// GlStateManager.matrixMode(GL11.GL_MODELVIEW);
// GlStateManager.translate(pPosX, pPosY, 1050.0D);
// GlStateManager.scale(1.0F, 1.0F, -1.0F);
//
// GlStateManager.pushMatrix();
// GlStateManager.translate(0.0D, 0.0D, 1000.0D);
// GlStateManager.scale(pScale, pScale, pScale);
//
// Quaternionf zp = Axis.ZP.rotationDegrees(180.0F);
// Quaternionf xp = Axis.XP.rotationDegrees(-10);
// zp.mul(xp);
// GlStateManager.rotate(j2l(zp));
//
// // 保存玩家状态
// float yBodyRot = player.renderYawOffset;
// float yRot = player.rotationYaw;
// float xRot = player.rotationPitch;
// float yHeadRotO = player.prevRotationYawHead;
// float yHeadRot = player.rotationYawHead;
//
// // 0-3 是盔甲
// ItemStack[] itemStacks = new ItemStack[6];
// itemStacks[0] = player.inventory.armorItemInSlot(3); // 头盔
// itemStacks[1] = player.inventory.armorItemInSlot(2);
// itemStacks[2] = player.inventory.armorItemInSlot(1);
// itemStacks[3] = player.inventory.armorItemInSlot(0);
// itemStacks[4] = player.inventory.getCurrentItem();
// itemStacks[5] = BackhandCompat.getOffhandItem(player);
// // 清空玩家物品以避免在模型上渲染
// player.inventory.mainInventory[player.inventory.currentItem] = null;
// BackhandCompat.setOffhandItem(player, null);
// for (int i = 0; i < 4; i++) {
// player.inventory.armorInventory[i] = null;
// }
//
// // 设置渲染状态
// player.renderYawOffset = 200;
// player.rotationYaw = 180;
// player.rotationPitch = 0;
// player.rotationYawHead = player.rotationYaw;
// player.prevRotationYawHead = player.rotationYaw;
//
// RenderHelper.enableGUIStandardItemLighting();
// RenderManager dispatcher = RenderManager.instance;
// // TODO camera shadow
// // xp.conjugate();
// // dispatcher.overrideCameraOrientation(xp);
// // dispatcher.setRenderShadow(false);
//
// // renderer.doRender();
//
// // 恢复状态
// // dispatcher.setRenderShadow(true);
//
// player.renderYawOffset = yBodyRot;
// player.rotationYaw = yRot;
// player.rotationPitch = xRot;
// player.prevRotationYawHead = yHeadRotO;
// player.rotationYawHead = yHeadRot;
//
// player.inventory.armorInventory[3] = itemStacks[0];
// player.inventory.armorInventory[2] = itemStacks[1];
// player.inventory.armorInventory[1] = itemStacks[2];
// player.inventory.armorInventory[0] = itemStacks[3];
// player.inventory.mainInventory[player.inventory.currentItem] = itemStacks[4];
// BackhandCompat.setOffhandItem(player, itemStacks[5]);
//
// GlStateManager.popMatrix();
// GlStateManager.popMatrix();
// RenderHelper.enableStandardItemLighting();
// }
//
// public static void renderPlayerEntity(EntityPlayer player, double posX, double posY, float scale, float yawOffset,
// int z) {
// GlStateManager.pushMatrix();
// GlStateManager.matrixMode(GL11.GL_MODELVIEW);
// GlStateManager.translate(posX + scale * 0.5, posY + scale * 2, z);
// GlStateManager.scale(1, 1, -1);
//
// GlStateManager.pushMatrix();
// GlStateManager.scale(scale, scale, scale);
// Quaternionf zRot = Axis.ZP.rotationDegrees(180.0F);
// Quaternionf yRot = Axis.YP.rotationDegrees(player.renderYawOffset + yawOffset - 180);
// zRot.mul(yRot);
// GlStateManager.rotate(j2l(zRot));
//
// RenderHelper.enableGUIStandardItemLighting();
// RenderManager dispatcher = RenderManager.instance;
//
// yRot.conjugate();
// // dispatcher.overrideCameraOrientation(yRot);
// // dispatcher.setRenderShadow(false);
//
// dispatcher.renderEntityWithPosYaw(player, 0, 0, 0.0D, 0.0F, 1.0F);
//
// // dispatcher.setRenderShadow(true);
//
// GlStateManager.popMatrix();
// GlStateManager.popMatrix();
// RenderHelper.enableStandardItemLighting();
// }
//
// private static Quaternion j2l(Quaternionf jomlQuat) {
// return Utils.j2l(jomlQuat);
// }
// }
