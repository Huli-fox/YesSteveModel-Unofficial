package com.fox.ysmu.client.renderer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import org.jetbrains.annotations.Nullable;

import com.fox.ysmu.client.entity.CustomPlayerEntity;
import com.fox.ysmu.client.model.CustomPlayerModel;
import com.fox.ysmu.client.renderer.layer.CustomPlayerItemInHandLayer;
import com.fox.ysmu.data.NPCData;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.fox.ysmu.event.api.SpecialPlayerRenderEvent;
import com.fox.ysmu.util.ModelIdUtil;

import it.unimi.dsi.fastutil.Pair;
import software.bernie.geckolib3.geo.GeoReplacedEntityRenderer;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.resource.GeckoLibCache;

public class CustomPlayerRenderer extends GeoReplacedEntityRenderer<CustomPlayerEntity> {

    private GeoModel geoModel;

    @SuppressWarnings("all")
    public CustomPlayerRenderer() {
        super(new CustomPlayerModel(), new CustomPlayerEntity());
        addLayer(new CustomPlayerItemInHandLayer<>(this));
        // addLayer(new CustomPlayerElytraLayer<>(this));
    }

    @Override
    public void doRender(EntityLivingBase entityObj, double x, double y, double z, float entityYaw,
        float partialTicks) {
        System.out.println("This is the overridden doRender method!");
        if (this.animatable != null && entityObj instanceof EntityPlayer player) {
            ExtendedModelInfo eep = ExtendedModelInfo.get(player);
            if (eep != null) {
                this.animatable.setPlayer(player);
                if (NPCData.contains(player.getUniqueID())) {
                    Pair<ResourceLocation, ResourceLocation> data = NPCData.getData(player.getUniqueID());
                    this.animatable.setMainModel(ModelIdUtil.getMainId(data.left()));
                    this.animatable.setTexture(data.right());
                } else {
                    this.animatable.setMainModel(ModelIdUtil.getMainId(eep.getModelId()));
                    this.animatable.setTexture(eep.getSelectTexture());
                }
            }
            if (MinecraftForge.EVENT_BUS.post(
                new SpecialPlayerRenderEvent(
                    player,
                    this.animatable,
                    ModelIdUtil.getModelIdFromMainId(this.animatable.getMainModel())))) {
                return;
            }
        }
        ResourceLocation location = this.modelProvider.getModelLocation(animatable);
        GeoModel geoModel = GeckoLibCache.getInstance()
            .getGeoModels()
            .get(location);
        if (geoModel != null) {
            this.geoModel = geoModel;
            super.doRender(entityObj, x, y, z, entityYaw, partialTicks);
        }
    }

    // @Override
    // public RenderType getRenderType(Object animatable, float partialTick, PoseStack poseStack, @Nullable
    // MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight, ResourceLocation texture) {
    // return RenderType.entityTranslucent(texture);
    // }

    // @Override
    // public boolean shouldShowName(Entity entity) {
    // double distance = this.entityRenderDispatcher.distanceToSqr(entity);
    // float renderDistance = entity.isDiscrete() ? 32.0F : 64.0F;
    // if (distance >= (double) (renderDistance * renderDistance)) {
    // return false;
    // } else {
    // Minecraft minecraft = Minecraft.getInstance();
    // LocalPlayer player = minecraft.player;
    // if (player == null) {
    // return false;
    // }
    // boolean invisible = !entity.isInvisibleTo(player);
    // if (entity != player) {
    // Team team1 = entity.getTeam();
    // Team team2 = player.getTeam();
    // if (team1 != null) {
    // Team.Visibility team$visibility = team1.getNameTagVisibility();
    // return switch (team$visibility) {
    // case ALWAYS -> invisible;
    // case NEVER -> false;
    // case HIDE_FOR_OTHER_TEAMS ->
    // team2 == null ? invisible : team1.isAlliedTo(team2) && (team1.canSeeFriendlyInvisibles() || invisible);
    // case HIDE_FOR_OWN_TEAM -> team2 == null ? invisible : !team1.isAlliedTo(team2) && invisible;
    // };
    // }
    // }
    // return Minecraft.renderNames() && entity != minecraft.getCameraEntity() && invisible && !entity.isVehicle();
    // }
    // }

    @Override
    @SuppressWarnings("all")
    protected void func_147906_a(Entity entity, String p_147906_2_, double p_147906_3_, double p_147906_5_,
        double p_147906_7_, int p_147906_9_) {
        if (entity instanceof EntityPlayer player) {
            double distance = player.getDistanceSqToEntity(this.renderManager.livingPlayer);
            if (distance < 100) {
                Scoreboard scoreboard = player.getWorldScoreboard();
                ScoreObjective objective = scoreboard.func_96539_a(2);
                if (objective != null) {
                    Score score = scoreboard.func_96529_a(player.getCommandSenderName(), objective);
                    String scoreText = Integer.toString(score.getScorePoints())
                        .concat(" ")
                        .concat(objective.getDisplayName());
                    super.func_147906_a(player, scoreText, p_147906_3_, p_147906_5_ + 0.26D, p_147906_7_, 100);
                }
            }
            super.func_147906_a(entity, p_147906_2_, p_147906_3_, p_147906_5_, p_147906_7_, p_147906_9_);
        }
    }

    // @Override
    // public float getWidthScale(Object animatable) {
    // if (this.animatable != null) {
    // return this.animatable.getWidthScale();
    // }
    // return super.getWidthScale(animatable);
    // }

    // @Override
    // public float getHeightScale(Object animatable) {
    // if (this.animatable != null) {
    // return this.animatable.getHeightScale();
    // }
    // return super.getHeightScale(animatable);
    // }

    public CustomPlayerEntity getCustomPlayerEntity() {
        return this.animatable;
    }

    @Nullable
    public GeoModel getGeoModel() {
        return geoModel;
    }
}
