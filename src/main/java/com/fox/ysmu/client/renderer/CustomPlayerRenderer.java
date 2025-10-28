package com.fox.ysmu.client.renderer;

import com.fox.ysmu.bukkit.client.NPCData;
import com.fox.ysmu.client.entity.CustomPlayerEntity;
import com.fox.ysmu.client.model.CustomPlayerModel;
import com.fox.ysmu.client.renderer.layer.CustomPlayerElytraLayer;
import com.fox.ysmu.client.renderer.layer.CustomPlayerItemInHandLayer;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.event.api.SpecialPlayerRenderEvent;
import com.fox.ysmu.geckolib3.geo.GeoReplacedEntityRenderer;
import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.fox.ysmu.geckolib3.resource.GeckoLibCache;
import com.fox.ysmu.util.Keep;
import com.fox.ysmu.util.ModelIdUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

public class CustomPlayerRenderer extends GeoReplacedEntityRenderer<CustomPlayerEntity> {
    private GeoModel geoModel;

    @SuppressWarnings("all")
    public CustomPlayerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CustomPlayerModel(), new CustomPlayerEntity());
        addLayer(new CustomPlayerItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
        addLayer(new CustomPlayerElytraLayer<>(this, ctx.getModelSet()));
    }

    @Override
    @Keep
    public void render(Entity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (this.animatable != null && entity instanceof Player player) {
            ExtendedModelInfo eep = ExtendedModelInfo.get(player);
            if (eep != null) {
                this.animatable.setPlayer(player);
                if (NPCData.contains(player.getUUID())) {
                    Pair<ResourceLocation, ResourceLocation> data = NPCData.getData(player.getUUID());
                    this.animatable.setMainModel(ModelIdUtil.getMainId(data.left()));
                    this.animatable.setTexture(data.right());
                } else {
                    this.animatable.setMainModel(ModelIdUtil.getMainId(eep.getModelId()));
                    this.animatable.setTexture(eep.getSelectTexture());
                }
            }
            if (MinecraftForge.EVENT_BUS.post(new SpecialPlayerRenderEvent(player, this.animatable, ModelIdUtil.getModelIdFromMainId(this.animatable.getMainModel())))) {
                return;
            }
        }
        ResourceLocation location = this.modelProvider.getModelLocation(animatable);
        GeoModel geoModel = GeckoLibCache.getInstance().getGeoModels().get(location);
        if (geoModel != null) {
            this.geoModel = geoModel;
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }
    }

    @Override
    @Keep
    public RenderType getRenderType(Object animatable, float partialTick, PoseStack poseStack, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    @Keep
    public boolean shouldShowName(Entity entity) {
        double distance = this.entityRenderDispatcher.distanceToSqr(entity);
        float renderDistance = entity.isDiscrete() ? 32.0F : 64.0F;
        if (distance >= (double) (renderDistance * renderDistance)) {
            return false;
        } else {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null) {
                return false;
            }
            boolean invisible = !entity.isInvisibleTo(player);
            if (entity != player) {
                Team team1 = entity.getTeam();
                Team team2 = player.getTeam();
                if (team1 != null) {
                    Team.Visibility team$visibility = team1.getNameTagVisibility();
                    return switch (team$visibility) {
                        case ALWAYS -> invisible;
                        case NEVER -> false;
                        case HIDE_FOR_OTHER_TEAMS ->
                                team2 == null ? invisible : team1.isAlliedTo(team2) && (team1.canSeeFriendlyInvisibles() || invisible);
                        case HIDE_FOR_OWN_TEAM -> team2 == null ? invisible : !team1.isAlliedTo(team2) && invisible;
                    };
                }
            }
            return Minecraft.renderNames() && entity != minecraft.getCameraEntity() && invisible && !entity.isVehicle();
        }
    }

    @Override
    @Keep
    @SuppressWarnings("all")
    protected void renderNameTag(Entity entity, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        double distance = this.entityRenderDispatcher.distanceToSqr(entity);
        poseStack.pushPose();
        if (distance < 100 && entity instanceof Player player) {
            Scoreboard scoreboard = player.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(2);
            if (objective != null) {
                Score score = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
                super.renderNameTag(player, (Component.literal(Integer.toString(score.getScore()))).append(" ").append(objective.getDisplayName()), poseStack, buffer, packedLight);
                poseStack.translate(0, 9.0 * 1.15 * 0.025, 0);
            }
        }
        super.renderNameTag(entity, displayName, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    @Keep
    public float getWidthScale(Object animatable) {
        if (this.animatable != null) {
            return this.animatable.getWidthScale();
        }
        return super.getWidthScale(animatable);
    }

    @Override
    @Keep
    public float getHeightScale(Object animatable) {
        if (this.animatable != null) {
            return this.animatable.getHeightScale();
        }
        return super.getHeightScale(animatable);
    }

    public CustomPlayerEntity getCustomPlayerEntity() {
        return this.animatable;
    }

    @Nullable
    public GeoModel getGeoModel() {
        return geoModel;
    }
}
