package com.fox.ysmu.client.renderer;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.fox.ysmu.client.ClientProxy;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.event.api.SpecialPlayerRenderEvent;
import com.fox.ysmu.util.AnimatableCacheUtil;
import com.fox.ysmu.util.ModelIdUtil;
import net.geckominecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.Project;

import com.fox.ysmu.Config;
import com.fox.ysmu.client.entity.CustomPlayerEntity;
import com.fox.ysmu.compat.BackhandCompat;

import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.geo.render.built.GeoQuad;
import software.bernie.geckolib3.geo.render.built.GeoVertex;
import software.bernie.geckolib3.resource.GeckoLibCache;

public final class FirstPersonHandRenderer {

    private static final String RIGHT_ARM = "RightArm";
    private static final float HAND_SWING_SCALE = 0.8F;
    private static final float GECKO_ARM_TO_BIPED_X = -0.5F;
    private static final float GECKO_ARM_SHOULDER_TO_BIPED_ORIGIN_Y = 29.0F / 16.0F;
    private static final float GECKO_ARM_TO_BIPED_Z = 0.0F;
    private static final float GECKO_ARM_ROLL_DEGREES = 180.0F;
    private static final ArmRollPivot DEFAULT_ARM_ROLL_PIVOT = new ArmRollPivot(2.0F / 16.0F, 0.0F);

    private FirstPersonHandRenderer() {}

    public static boolean shouldRenderCustomHand(Minecraft mc, EntityPlayer player, ItemRenderer itemRenderer) {
        return !Config.DISABLE_SELF_MODEL
            && !Config.DISABLE_SELF_HANDS
            && mc != null
            && mc.entityRenderer != null
            && mc.playerController != null
            && player instanceof EntityPlayerSP
            && itemRenderer != null
            && mc.theWorld != null
            && mc.renderViewEntity != null
            && !mc.gameSettings.hideGUI
            && mc.gameSettings.thirdPersonView == 0
            && !player.isInvisible()
            && !mc.renderViewEntity.isPlayerSleeping()
            && !mc.playerController.enableEverythingIsScrewedUpMode()
            && mc.entityRenderer.debugViewDirection <= 0
            && mc.entityRenderer.cameraZoom == 1.0D
            && !BackhandCompat.isRenderingOffhand(player)
            && itemRenderer.itemToRender == null;
    }

    public static boolean hasRenderableRightArm(GeoModel geoModel) {
        return findRightArmBone(geoModel).isPresent();
    }

    public static boolean tryRender(RenderHandEvent event, Minecraft mc, EntityPlayer player,
        ItemRenderer itemRenderer) {
        CustomHandRenderContext context = getCustomHandRenderContext(mc, player, itemRenderer);
        if (context == null) {
            return false;
        }

        event.setCanceled(true);
        render(event, mc, player, itemRenderer, context.renderer, context.geoModel, context.customPlayer);
        return true;
    }

    public static boolean tryRenderInActiveFirstPersonPass(Minecraft mc, ItemRenderer itemRenderer, float partialTicks,
        boolean renderOffhand) {
        EntityPlayer player = mc == null ? null : mc.thePlayer;
        CustomHandRenderContext context = getCustomHandRenderContext(mc, player, itemRenderer);
        if (context == null) {
            return false;
        }

        renderFirstPersonItems(
            mc,
            player,
            itemRenderer,
            context.renderer,
            context.geoModel,
            context.customPlayer,
            partialTicks,
            renderOffhand);
        return true;
    }

    public static void render(RenderHandEvent event, Minecraft mc, EntityPlayer player, ItemRenderer itemRenderer,
        CustomPlayerRenderer renderer, GeoModel geoModel, CustomPlayerEntity customPlayer) {
        float partialTicks = event.partialTicks;
        EntityRenderer entityRenderer = mc.entityRenderer;

        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        setupFirstPersonHandCamera(event, mc, entityRenderer, partialTicks);

        GL11.glPushMatrix();
        try {
            entityRenderer.hurtCameraEffect(partialTicks);
            if (mc.gameSettings.viewBobbing) {
                entityRenderer.setupViewBobbing(partialTicks);
            }

            entityRenderer.enableLightmap((double) partialTicks);
            try {
                renderFirstPersonItems(mc, player, itemRenderer, renderer, geoModel, customPlayer, partialTicks, true);
            } finally {
                entityRenderer.disableLightmap((double) partialTicks);
            }
        } finally {
            GL11.glPopMatrix();
        }

        renderVanillaOverlays(mc, entityRenderer, itemRenderer, partialTicks);
    }

    private static void setupFirstPersonHandCamera(RenderHandEvent event, Minecraft mc,
        EntityRenderer entityRenderer, float partialTicks) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        if (mc.gameSettings.anaglyph) {
            GL11.glTranslatef((float) (-(event.renderPass * 2 - 1)) * 0.07F, 0.0F, 0.0F);
        }

        if (entityRenderer.cameraZoom != 1.0D) {
            GL11.glTranslatef((float) entityRenderer.cameraYaw, (float) (-entityRenderer.cameraPitch), 0.0F);
            GL11.glScaled(entityRenderer.cameraZoom, entityRenderer.cameraZoom, 1.0D);
        }

        float farPlane = entityRenderer.farPlaneDistance > 0.0F
            ? entityRenderer.farPlaneDistance
            : (float) (mc.gameSettings.renderDistanceChunks * 16);
        Project.gluPerspective(
            entityRenderer.getFOVModifier(partialTicks, false),
            (float) mc.displayWidth / (float) mc.displayHeight,
            0.05F,
            farPlane * 2.0F);

        if (mc.playerController.enableEverythingIsScrewedUpMode()) {
            GL11.glScalef(1.0F, 0.6666667F, 1.0F);
        }

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        if (mc.gameSettings.anaglyph) {
            GL11.glTranslatef((float) (event.renderPass * 2 - 1) * 0.1F, 0.0F, 0.0F);
        }
    }

    private static void renderFirstPersonItems(Minecraft mc, EntityPlayer player, ItemRenderer itemRenderer,
        CustomPlayerRenderer renderer, GeoModel geoModel, CustomPlayerEntity customPlayer, float partialTicks,
        boolean renderOffhand) {
        applyVanillaHandLighting(player, partialTicks);
        applyVanillaArmViewSmoothing(player, partialTicks);
        setupLightmap(mc, player);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        try {
            renderCustomEmptyMainHand(mc, player, itemRenderer, renderer, geoModel, customPlayer, partialTicks);
            if (renderOffhand) {
                BackhandCompat.renderOffhand(partialTicks);
            }
        } finally {
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.disableStandardItemLighting();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void applyVanillaHandLighting(EntityPlayer player, float partialTicks) {
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;

        GL11.glPushMatrix();
        GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GL11.glPopMatrix();
    }

    private static void applyVanillaArmViewSmoothing(EntityPlayer player, float partialTicks) {
        EntityPlayerSP playerSp = (EntityPlayerSP) player;
        float armPitch = playerSp.prevRenderArmPitch + (playerSp.renderArmPitch - playerSp.prevRenderArmPitch)
            * partialTicks;
        float armYaw = playerSp.prevRenderArmYaw + (playerSp.renderArmYaw - playerSp.prevRenderArmYaw)
            * partialTicks;

        GL11.glRotatef((player.rotationPitch - armPitch) * 0.1F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef((player.rotationYaw - armYaw) * 0.1F, 0.0F, 1.0F, 0.0F);
    }

    private static void setupLightmap(Minecraft mc, EntityPlayer player) {
        int light = mc.theWorld.getLightBrightnessForSkyBlocks(
            MathHelper.floor_double(player.posX),
            MathHelper.floor_double(player.posY),
            MathHelper.floor_double(player.posZ),
            0);
        OpenGlHelper.setLightmapTextureCoords(
            OpenGlHelper.lightmapTexUnit,
            (float) (light % 65536),
            (float) (light / 65536));
    }

    private static void renderCustomEmptyMainHand(Minecraft mc, EntityPlayer player, ItemRenderer itemRenderer,
        CustomPlayerRenderer renderer, GeoModel geoModel, CustomPlayerEntity customPlayer, float partialTicks) {
        Optional<GeoBone> rightArm = findRightArmBone(geoModel);
        if (!rightArm.isPresent()) {
            return;
        }

        GL11.glPushMatrix();
        try {
            applyVanillaEmptyHandTransform(player, itemRenderer, partialTicks);
            mc.getTextureManager()
                .bindTexture(customPlayer.getTexture());
            prepareCustomArmState();
            try {
                alignGeckoArmToVanillaBipedArm(rightArm.get());
                renderRightArmBone(renderer, rightArm.get(), customPlayer);
            } finally {
                restoreCustomArmState();
            }
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void applyVanillaEmptyHandTransform(EntityPlayer player, ItemRenderer itemRenderer,
        float partialTicks) {
        float equippedProgress = itemRenderer.prevEquippedProgress
            + (itemRenderer.equippedProgress - itemRenderer.prevEquippedProgress) * partialTicks;
        float swing = player.getSwingProgress(partialTicks);
        float swingSin = MathHelper.sin(swing * (float) Math.PI);
        float swingRootSin = MathHelper.sin(MathHelper.sqrt_float(swing) * (float) Math.PI);

        GL11.glTranslatef(
            -swingRootSin * 0.3F,
            MathHelper.sin(MathHelper.sqrt_float(swing) * (float) Math.PI * 2.0F) * 0.4F,
            -swingSin * 0.4F);
        GL11.glTranslatef(
            0.8F * HAND_SWING_SCALE,
            -0.75F * HAND_SWING_SCALE - (1.0F - equippedProgress) * 0.6F,
            -0.9F * HAND_SWING_SCALE);
        GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        swingSin = MathHelper.sin(swing * swing * (float) Math.PI);
        swingRootSin = MathHelper.sin(MathHelper.sqrt_float(swing) * (float) Math.PI);
        GL11.glRotatef(swingRootSin * 70.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-swingSin * 20.0F, 0.0F, 0.0F, 1.0F);

        GL11.glTranslatef(-1.0F, 3.6F, 3.5F);
        GL11.glRotatef(120.0F, 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(200.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(5.6F, 0.0F, 0.0F);
    }

    private static void prepareCustomArmState() {
        GlStateManager.enableBlend();
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.disableCull();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void restoreCustomArmState() {
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void alignGeckoArmToVanillaBipedArm(GeoBone rightArm) {
        ArmRollPivot rollPivot = findArmRollPivot(rightArm);
        GL11.glTranslatef(
            GECKO_ARM_TO_BIPED_X,
            GECKO_ARM_SHOULDER_TO_BIPED_ORIGIN_Y,
            GECKO_ARM_TO_BIPED_Z);
        GL11.glScalef(1.0F, -1.0F, 1.0F);
        GL11.glTranslatef(rollPivot.x, 0.0F, rollPivot.z);
        GL11.glRotatef(GECKO_ARM_ROLL_DEGREES, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-rollPivot.x, 0.0F, -rollPivot.z);
    }

    private static void renderRightArmBone(CustomPlayerRenderer renderer, GeoBone rightArm,
        CustomPlayerEntity customPlayer) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_QUADS);
        renderer.renderRecursively(tessellator, customPlayer, rightArm, 1.0F, 1.0F, 1.0F, 1.0F);
        tessellator.draw();
    }

    private static ArmRollPivot findArmRollPivot(GeoBone rightArm) {
        ArmRollBounds bounds = new ArmRollBounds();
        collectArmRollBounds(rightArm, bounds);
        return bounds.hasValues()
            ? new ArmRollPivot((bounds.minX + bounds.maxX) * 0.5F, (bounds.minZ + bounds.maxZ) * 0.5F)
            : DEFAULT_ARM_ROLL_PIVOT;
    }

    private static void collectArmRollBounds(GeoBone bone, ArmRollBounds bounds) {
        for (GeoCube cube : bone.childCubes) {
            for (GeoQuad quad : cube.quads) {
                if (quad == null) {
                    continue;
                }
                for (GeoVertex vertex : quad.vertices) {
                    bounds.include(vertex.position.x, vertex.position.z);
                }
            }
        }
        for (GeoBone child : bone.childBones) {
            collectArmRollBounds(child, bounds);
        }
    }

    private static Optional<GeoBone> findRightArmBone(GeoModel geoModel) {
        if (geoModel == null) {
            return Optional.empty();
        }
        Optional<GeoBone> rightArm = geoModel.getTopLevelBone(RIGHT_ARM);
        return rightArm.isPresent() ? rightArm : geoModel.getBone(RIGHT_ARM);
    }

    private static void renderVanillaOverlays(Minecraft mc, EntityRenderer entityRenderer, ItemRenderer itemRenderer,
        float partialTicks) {
        if (mc.gameSettings.thirdPersonView == 0 && !mc.renderViewEntity.isPlayerSleeping()) {
            itemRenderer.renderOverlays(partialTicks);
            entityRenderer.hurtCameraEffect(partialTicks);
        }

        if (mc.gameSettings.viewBobbing) {
            entityRenderer.setupViewBobbing(partialTicks);
        }
    }

    private static final class ArmRollPivot {
        private final float x;
        private final float z;

        private ArmRollPivot(float x, float z) {
            this.x = x;
            this.z = z;
        }
    }

    private static final class ArmRollBounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        private void include(float x, float z) {
            this.minX = Math.min(this.minX, x);
            this.maxX = Math.max(this.maxX, x);
            this.minZ = Math.min(this.minZ, z);
            this.maxZ = Math.max(this.maxZ, z);
        }

        private boolean hasValues() {
            return this.minX != Float.POSITIVE_INFINITY;
        }
    }

    private static CustomHandRenderContext getCustomHandRenderContext(Minecraft mc, EntityPlayer player,
        ItemRenderer itemRenderer) {
        if (!shouldRenderCustomHand(mc, player, itemRenderer)) {
            return null;
        }

        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep == null) {
            return null;
        }
        ResourceLocation modelId = eep.getModelId();
        if (modelId == null) {
            return null;
        }
        GeoModel geoModel = GeckoLibCache.getInstance()
            .getGeoModels()
            .get(ModelIdUtil.getArmId(modelId));
        if (!hasRenderableRightArm(geoModel)) {
            return null;
        }
        CustomPlayerRenderer renderer = ClientProxy.getInstance();
        if (renderer == null) {
            return null;
        }
        CustomPlayerEntity customPlayer = getCustomHandPlayer(modelId, eep, player);
        if (customPlayer == null) {
            return null;
        }
        if (MinecraftForge.EVENT_BUS.post(new SpecialPlayerRenderEvent(player, customPlayer, modelId))) {
            return null;
        }
        return new CustomHandRenderContext(renderer, geoModel, customPlayer);
    }

    private static CustomPlayerEntity getCustomHandPlayer(ResourceLocation modelId, ExtendedModelInfo eep,
        EntityPlayer player) {
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
        if (animatable instanceof CustomPlayerEntity customPlayer) {
            customPlayer.setPlayer(player);
            customPlayer.setMainModel(ModelIdUtil.getMainId(modelId));
            customPlayer.setTexture(eep.getSelectTexture());
            return customPlayer;
        }
        return null;
    }

    private static final class CustomHandRenderContext {
        private final CustomPlayerRenderer renderer;
        private final GeoModel geoModel;
        private final CustomPlayerEntity customPlayer;

        private CustomHandRenderContext(CustomPlayerRenderer renderer, GeoModel geoModel,
            CustomPlayerEntity customPlayer) {
            this.renderer = renderer;
            this.geoModel = geoModel;
            this.customPlayer = customPlayer;
        }
    }
}
