package com.fox.ysmu.client;

import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.geckominecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;

import org.joml.Quaternionf;
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

    public static int DEBUG_BG_WIDTH = 1000;
    private static final Pattern INT_REG = Pattern.compile("^[0-9]*$");

    private static ModelBiped MODEL_BIPED;

    private static final String LEFT_ARM = "LeftArm";
    private static final String RIGHT_ARM = "RightArm";
    private static final String BACKGROUND_BONE = "Background";
    private static boolean alreadyRenderedThisFrame = false;

    @SubscribeEvent
    public static void onTextureStitchEventPost(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() == 0) {
            ClientModelManager.loadDefaultModel();
            ClientModelManager.CACHE_MD5.forEach(RequestLoadModel::loadModel);
            Matcher matcher = INT_REG.matcher(I18n.format("molang.yes_steve_model.bg_width"));
            if (matcher.matches()) {
                DEBUG_BG_WIDTH = Integer.parseInt(I18n.format("molang.yes_steve_model.bg_width"));
            }
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
        renderer.doRender(
            event.entityPlayer,
            event.entityPlayer.posX,
            event.entityPlayer.posY,
            event.entityPlayer.posZ,
            event.entityPlayer.rotationYaw,
            event.partialRenderTick);
    }

    @SubscribeEvent
    public static void onRender3rdPersonHand(RenderPlayerEvent.Specials.Post event) {
        MODEL_BIPED = event.renderer.modelBipedMain;
    }

    @SubscribeEvent
    // TODO 它在每一帧的世界渲染结束后触发，适合用来重置每帧的状态?
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        alreadyRenderedThisFrame = false;
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (Config.DISABLE_SELF_MODEL || Config.DISABLE_SELF_HANDS) {
            return;
        }

        AbstractClientPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            return;
        }

        event.setCanceled(true);

        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep != null) {
            ResourceLocation modelId = eep.getModelId();
            // 注意：我们加载的是包含手臂和背景的完整第一人称模型
            GeoModel geoModel = GeckoLibCache.getInstance()
                .getGeoModels()
                .get(ModelIdUtil.getArmId(modelId));
            if (geoModel == null) {
                return;
            }

            CustomPlayerRenderer rendererInstance = ClientProxy.getInstance();
            if (rendererInstance == null) {
                return;
            }

            // --- 通用渲染设置 ---
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

            if (!(animatable instanceof CustomPlayerEntity customPlayer)) {
                return;
            }

            customPlayer.setTexture(eep.getSelectTexture());
            if (MinecraftForge.EVENT_BUS.post(new SpecialPlayerRenderEvent(player, customPlayer, modelId))) {
                return;
            }
            Tessellator tess = Tessellator.instance;

            // --- 渲染逻辑 ---
            GlStateManager.pushMatrix();

            // 1. 背景渲染和视角摇晃（每帧仅一次）
            if (!alreadyRenderedThisFrame) {
                alreadyRenderedThisFrame = true;

                // 应用视角摇晃
                GlStateManager.pushMatrix();
                if (Minecraft.getMinecraft().gameSettings.viewBobbing) {
                    bobView(event.partialTicks, player);
                }

                // 渲染背景骨骼
                if (geoModel.hasTopLevelBone(BACKGROUND_BONE)) {
                    GlStateManager.translate(0, -1.5, 0); // 背景模型的特定位移
                    geoModel.getTopLevelBone(BACKGROUND_BONE)
                        .ifPresent(bone -> rendererInstance.renderRecursively(tess, animatable, bone, 1, 1, 1, 1));
                }
                GlStateManager.popMatrix();
            }

            // 2. 手臂渲染（每次事件触发时都执行）
            // 渲染右臂
            GlStateManager.pushMatrix();
            GlStateManager.translate(-0.25, 1.8, 0);
            GlStateManager.scale(-1, -1, 1);
            geoModel.getTopLevelBone(RIGHT_ARM)
                .ifPresent(bone -> rendererInstance.renderRecursively(tess, animatable, bone, 1, 1, 1, 1));
            GlStateManager.popMatrix();

            GlStateManager.popMatrix();
        }
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
        return modelId.getResourcePath()
            .equals("steve"); // TODO steve
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
