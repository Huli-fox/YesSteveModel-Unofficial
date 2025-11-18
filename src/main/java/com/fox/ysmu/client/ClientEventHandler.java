package com.fox.ysmu.client;

import java.util.concurrent.ExecutionException;

import com.fox.ysmu.client.gui.ExtraPlayerConfigScreen;
import com.fox.ysmu.util.RenderUtil;
import net.geckominecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;

import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Quaternion;

import com.fox.ysmu.Config;
import com.fox.ysmu.client.entity.CustomPlayerEntity;
import com.fox.ysmu.client.renderer.CustomPlayerRenderer;
import com.fox.ysmu.compat.Axis;
import com.fox.ysmu.compat.Utils;
import com.fox.ysmu.data.NPCData;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.event.api.SpecialPlayerRenderEvent;
import com.fox.ysmu.network.NetworkHandler;
import com.fox.ysmu.network.message.RequestLoadModel;
import com.fox.ysmu.network.message.SetPlayAnimation;
import com.fox.ysmu.util.AnimatableCacheUtil;
import com.fox.ysmu.util.ModelIdUtil;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.relauncher.Side;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.resource.GeckoLibCache;

@EventBusSubscriber(side = Side.CLIENT)
public class ClientEventHandler {

    private static ModelBiped MODEL_BIPED;

    private static final String LEFT_ARM = "LeftArm";
    private static final String RIGHT_ARM = "RightArm";
    private static final String BACKGROUND_BONE = "Background";
    private static boolean alreadyRenderedThisFrame = false;

    private static boolean EXTRA_PLAYER = false;

    @SubscribeEvent
    public static void onTextureStitchEventPost(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() == 0) {
            ClientModelManager.loadDefaultModel();
            ClientModelManager.CACHE_MD5.forEach(RequestLoadModel::loadModel);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(SpecialPlayerRenderEvent event) {
        EntityPlayer player = event.getPlayer();
        CustomPlayerEntity animatable = event.getCustomPlayer();
        if (isVanillaPlayer(event.getModelId()) && player instanceof AbstractClientPlayer clientPlayer) {
            animatable.setPlayer(player);
            animatable.setMainModel(ModelIdUtil.getMainId(event.getModelId()));
            ResourceLocation location = clientPlayer.getLocationSkin();
            animatable.setTexture(location);
        }
    }

    @SubscribeEvent
    public static void onRender(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.entityPlayer;
        EntityClientPlayerMP playerSelf = Minecraft.getMinecraft().thePlayer;
        if (player.equals(playerSelf) && Config.DISABLE_SELF_MODEL) {
            return;
        }
        if (!player.equals(playerSelf) && Config.DISABLE_OTHER_MODEL) {
            return;
        }
        event.setCanceled(true);
        CustomPlayerRenderer renderer = ClientProxy.getInstance();
        if ((Minecraft.getMinecraft().currentScreen != null || EXTRA_PLAYER) && player.equals(playerSelf)) {
            renderer.doRender(
                player,
                0,
                0 - player.yOffset,
                0,
                player.rotationYaw,
                1.0F);
        } else {
            float partialTicks = event.partialRenderTick;
            double ix = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
            double iy = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
            double iz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
            renderer.doRender(
                player,
                ix - RenderManager.renderPosX,
                iy - RenderManager.renderPosY - player.yOffset,
                iz - RenderManager.renderPosZ,
                player.rotationYaw,
                partialTicks);
        }
    }

    @SubscribeEvent
    public static void onRender3rdPersonHand(RenderPlayerEvent.Specials.Post event) {
        MODEL_BIPED = event.renderer.modelBipedMain;
    }

    // TODO 不完善的实现
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (Config.DISABLE_SELF_MODEL || Config.DISABLE_SELF_HANDS) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (mc.gameSettings.thirdPersonView != 0 || player.getHeldItem() != null) return;
        event.setCanceled(true);

        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep == null) return;
        ResourceLocation modelId = eep.getModelId();
        GeoModel geoModel = GeckoLibCache.getInstance().getGeoModels().get(ModelIdUtil.getArmId(modelId));
        if (geoModel == null) return;
        CustomPlayerRenderer renderer = ClientProxy.getInstance();
        if (renderer == null) return;
        IAnimatable animatable;
        try {
            animatable = AnimatableCacheUtil.ANIMATABLE_CACHE.get(modelId, () -> {
                CustomPlayerEntity entity = new CustomPlayerEntity();
                entity.setTexture(eep.getSelectTexture());
                return entity;
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (!(animatable instanceof CustomPlayerEntity customPlayer)) return;
        customPlayer.setTexture(eep.getSelectTexture());
        if (MinecraftForge.EVENT_BUS.post(new SpecialPlayerRenderEvent(player, customPlayer, modelId))) return;

        GlStateManager.pushMatrix();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.enableBlend(); // 开启混合，处理透明贴图
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); // 设置混合函数
        GlStateManager.disableCull(); // 禁用面剔除，可选，但可以防止模型内侧消失
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); // 重置颜色

        mc.getTextureManager().bindTexture(customPlayer.getTexture());

        float partialTicks = event.partialTicks;
        float interpolatedYaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
        float interpolatedPitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        GlStateManager.rotate(interpolatedYaw, 0.0F, -1.0F, 0.0F);
        GlStateManager.rotate(interpolatedPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-1, 0, 2);
        GlStateManager.scale(-1, -1, 1);
        GlStateManager.rotate(25, -1.5F, 3, -0.5F);
        GlStateManager.rotate(53, 0, 3, 0);
        GlStateManager.rotate(30, -1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(30, 0.0F, 0.0F, -1.0F);

        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_QUADS);
        geoModel.getTopLevelBone(RIGHT_ARM)
            .ifPresent(bone -> renderer.renderRecursively(tess, animatable, bone, 1, 1, 1, 1));
        tess.draw();

        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public static void onRenderScreen(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) {
            return;
        }
        if (Config.DISABLE_PLAYER_RENDER) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) {
            return;
        }
        if (mc.currentScreen instanceof ExtraPlayerConfigScreen) {
            return;
        }
        double posX = Config.PLAYER_POS_X;
        double posY = Config.PLAYER_POS_Y;
        float scale = (float) Config.PLAYER_SCALE;
        float yawOffset = (float) Config.PLAYER_YAW_OFFSET;
        EXTRA_PLAYER = true;
        RenderUtil.renderPlayerEntity(player, posX, posY, scale, yawOffset, -500);
        EXTRA_PLAYER = false;
    }

    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.KeyInputEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (isMoveKey() && player != null) {
            ExtendedModelInfo eep = ExtendedModelInfo.get(player);
            if (eep != null && eep.isPlayAnimation()) {
                NetworkHandler.CHANNEL.sendToServer(SetPlayAnimation.stop());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        NPCData.clear();
    }

    public static ModelBiped getModelBiped() {
        return MODEL_BIPED;
    }

    private static boolean isVanillaPlayer(ResourceLocation modelId) {
        return modelId.getResourcePath().equals("steve");
    }

    private static void bobView(float pPartialTicks, EntityPlayer player) {
        float walk = player.distanceWalkedModified - player.prevDistanceWalkedModified;
        float walk2 = -(player.distanceWalkedModified + walk * pPartialTicks);
        float lerp = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * pPartialTicks;
        GlStateManager.translate(
            -MathHelper.sin(walk2 * (float) Math.PI) * lerp * 0.5F,
            Math.abs(MathHelper.cos(walk2 * (float) Math.PI) * lerp),
            0.0D);
        GlStateManager.rotate(j2l(Axis.ZN.rotationDegrees(MathHelper.sin(walk2 * (float) Math.PI) * lerp * 3.0F)));
        GlStateManager.rotate(
            j2l(Axis.XN.rotationDegrees(Math.abs(MathHelper.cos(walk2 * (float) Math.PI - 0.2F) * lerp) * 5.0F)));
    }

    private static boolean isMoveKey() {
        KeyBinding[] keyBindings = Minecraft.getMinecraft().gameSettings.keyBindings;
        for (KeyBinding keyBinding : keyBindings) {
            if ((keyBinding == Minecraft.getMinecraft().gameSettings.keyBindForward
                || keyBinding == Minecraft.getMinecraft().gameSettings.keyBindBack
                || keyBinding == Minecraft.getMinecraft().gameSettings.keyBindLeft
                || keyBinding == Minecraft.getMinecraft().gameSettings.keyBindRight
                || keyBinding == Minecraft.getMinecraft().gameSettings.keyBindJump
                || keyBinding == Minecraft.getMinecraft().gameSettings.keyBindSneak) && keyBinding.isPressed()) {
                return true;
            }
        }
        return false;
    }

    private static Quaternion j2l(Quaternionf jomlQuat) {
        return Utils.j2l(jomlQuat);
    }
}
