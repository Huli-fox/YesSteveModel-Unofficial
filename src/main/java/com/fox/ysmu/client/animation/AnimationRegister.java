package com.fox.ysmu.client.animation;

import java.util.function.BiPredicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

import com.fox.ysmu.client.entity.CustomPlayerEntity;
import com.fox.ysmu.compat.BackhandCompat;
import com.fox.ysmu.compat.EtfuturumCompat;

import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.molang.LazyVariable;
import software.bernie.geckolib3.core.molang.MolangParser;
import software.bernie.geckolib3.model.provider.data.EntityModelData;
import software.bernie.geckolib3.resource.GeckoLibCache;
import software.bernie.geckolib3.util.MolangUtils;

public class AnimationRegister {

    private static final double MIN_SPEED = 0.05;

    public static void registerAnimationState() {
        register("death", ILoopType.EDefaultLoopTypes.PLAY_ONCE, Priority.HIGHEST, (player, event) -> player.isDead);
        // register("riptide", Priority.HIGHEST, (player, event) -> player.isAutoSpinAttack());
        register("sleep", Priority.HIGHEST, (player, event) -> player.isPlayerSleeping());
        // TODO 游泳，待检查
        register("swim", Priority.HIGHEST, (player, event) -> player.isInWater() && Math.abs(event.getLimbSwingAmount()) > MIN_SPEED);
        register("climb", Priority.HIGHEST, (player, event) -> player.isOnLadder() && Math.abs(player.motionY) > 0);
        register("climbing", Priority.HIGHEST, (player, event) -> player.isOnLadder());
        register("ride_pig", Priority.HIGH, (player, event) -> player.ridingEntity instanceof EntityPig);
        register("ride", Priority.HIGH, (player, event) -> player.isRiding() && !(player.ridingEntity instanceof EntityBoat));
        register("boat", Priority.HIGH, (player, event) -> player.ridingEntity instanceof EntityBoat);
        register("sit", Priority.HIGH, (player, event) -> player.isRiding());
        register("fly", Priority.HIGH, (player, event) -> player.capabilities.isFlying);
        register("elytra_fly", Priority.HIGH, (player, event) -> EtfuturumCompat.isFallFlying(player));
        register("swim_stand", Priority.NORMAL, (player, event) -> player.isInWater());
        register("attacked", ILoopType.EDefaultLoopTypes.PLAY_ONCE, Priority.NORMAL, (player, event) -> player.hurtTime > 0);
        register("jump", Priority.NORMAL, (player, event) -> !player.onGround && !player.isInWater());
        register("sneak", Priority.NORMAL, (player, event) -> player.onGround && player.isSneaking() && Math.abs(event.getLimbSwingAmount()) > MIN_SPEED);
        register("sneaking", Priority.NORMAL, (player, event) -> player.onGround && player.isSneaking());
        register("run", Priority.LOW, (player, event) -> player.onGround && player.isSprinting());
        register("walk", Priority.LOW, (player, event) -> player.onGround && event.getLimbSwingAmount() > MIN_SPEED);
        register("idle", Priority.LOWEST, (player, event) -> true);
    }

    @SuppressWarnings("deprecation")
    public static void registerVariables() {
        MolangParser parser = GeckoLibCache.getInstance().parser;

        parser.register(new LazyVariable("query.actor_count", 0));
        parser.register(new LazyVariable("query.anim_time", 0));

        parser.register(new LazyVariable("query.body_x_rotation", 0));
        parser.register(new LazyVariable("query.body_y_rotation", 0));
        parser.register(new LazyVariable("query.cardinal_facing_2d", 0));
        parser.register(new LazyVariable("query.distance_from_camera", 0));
        parser.register(new LazyVariable("query.equipment_count", 0));
        parser.register(new LazyVariable("query.eye_target_x_rotation", 0));
        parser.register(new LazyVariable("query.eye_target_y_rotation", 0));
        parser.register(new LazyVariable("query.ground_speed", 0));

        parser.register(new LazyVariable("query.has_cape", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.has_rider", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.head_x_rotation", 0));
        parser.register(new LazyVariable("query.head_y_rotation", 0));
        parser.register(new LazyVariable("query.health", 0));
        parser.register(new LazyVariable("query.hurt_time", 0));

        parser.register(new LazyVariable("query.is_eating", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_first_person", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_in_water", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_in_water_or_rain", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_jumping", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_on_fire", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_on_ground", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_playing_dead", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_riding", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_sleeping", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_sneaking", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_spectator", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_sprinting", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_swimming", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.is_using_item", MolangUtils.FALSE));
        parser.register(new LazyVariable("query.item_in_use_duration", 0));
        parser.register(new LazyVariable("query.item_max_use_duration", 0));
        parser.register(new LazyVariable("query.item_remaining_use_duration", 0));

        parser.register(new LazyVariable("query.life_time", 0));
        parser.register(new LazyVariable("query.max_health", 0));
        parser.register(new LazyVariable("query.modified_distance_moved", 0));
        parser.register(new LazyVariable("query.moon_phase", 0));

        parser.register(new LazyVariable("query.player_level", 0));
        parser.register(new LazyVariable("query.time_of_day", 0));
        parser.register(new LazyVariable("query.time_stamp", 0));
        parser.register(new LazyVariable("query.vertical_speed", 0));
        parser.register(new LazyVariable("query.walk_distance", 0));
        parser.register(new LazyVariable("query.yaw_speed", 0));

        parser.register(new LazyVariable("ysm.head_yaw", 0));
        parser.register(new LazyVariable("ysm.head_pitch", 0));
        parser.register(new LazyVariable("ysm.has_helmet", MolangUtils.FALSE));
        parser.register(new LazyVariable("ysm.has_chest_plate", MolangUtils.FALSE));
        parser.register(new LazyVariable("ysm.has_leggings", MolangUtils.FALSE));
        parser.register(new LazyVariable("ysm.has_boots", MolangUtils.FALSE));
        parser.register(new LazyVariable("ysm.has_mainhand", MolangUtils.FALSE));
        parser.register(new LazyVariable("ysm.has_offhand", MolangUtils.FALSE));

        parser.register(new LazyVariable("ysm.has_elytra", MolangUtils.FALSE));
        parser.register(new LazyVariable("ysm.elytra_rot_x", 0));
        parser.register(new LazyVariable("ysm.elytra_rot_y", 0));
        parser.register(new LazyVariable("ysm.elytra_rot_z", 0));

        parser.register(new LazyVariable("ysm.is_close_eyes", MolangUtils.FALSE));
        parser.register(new LazyVariable("ysm.is_passenger", MolangUtils.FALSE));
        parser.register(new LazyVariable("ysm.is_sleep", MolangUtils.FALSE));
        parser.register(new LazyVariable("ysm.is_sneak", MolangUtils.FALSE));
        // parser.register(new LazyVariable("ysm.is_riptide", MolangUtils.FALSE));

        parser.register(new LazyVariable("ysm.armor_value", 0));
        parser.register(new LazyVariable("ysm.hurt_time", 0));
        parser.register(new LazyVariable("ysm.food_level", 20));

        // parser.register(new LazyVariable("ysm.first_person_mod_hide", MolangUtils.FALSE));
    }

    public static void setParserValue(AnimationEvent<CustomPlayerEntity> animationEvent, MolangParser parser,
        EntityModelData data, EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return;
        }

        parser.setValue("query.actor_count", () -> mc.theWorld.loadedEntityList.size());
        parser.setValue("query.body_x_rotation", player.rotationPitch);
        parser.setValue("query.body_y_rotation", () -> MathHelper.wrapAngleTo180_float(player.rotationYaw));
        parser.setValue(
            "query.cardinal_facing_2d",
            () -> MathHelper.floor_double((double) (player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3);
        parser.setValue("query.distance_from_camera", () -> mc.renderViewEntity.getDistanceToEntity(player));
        parser.setValue("query.equipment_count", () -> getEquipmentCount(player));
        parser.setValue("query.eye_target_x_rotation", () -> player.rotationPitch);
        parser.setValue("query.eye_target_y_rotation", () -> player.rotationYaw);
        parser.setValue("query.ground_speed", () -> getGroundSpeed(player));

        parser.setValue("query.has_cape", () -> MolangUtils.booleanToFloat(hasCape(player)));
        parser.setValue("query.has_rider", () -> MolangUtils.booleanToFloat(player.riddenByEntity != null));
        parser.setValue("query.head_x_rotation", () -> data.netHeadYaw);
        parser.setValue("query.head_y_rotation", () -> data.headPitch);
        parser.setValue("query.health", player::getHealth);
        parser.setValue("query.hurt_time", () -> player.hurtTime);

        parser.setValue(
            "query.is_eating",
            () -> MolangUtils.booleanToFloat(
                player.getItemInUse() != null && player.getItemInUse()
                    .getItemUseAction() == EnumAction.eat));
        parser
            .setValue("query.is_first_person", () -> MolangUtils.booleanToFloat(mc.gameSettings.thirdPersonView == 0));
        parser.setValue("query.is_in_water", () -> MolangUtils.booleanToFloat(player.isInWater()));
        parser.setValue("query.is_in_water_or_rain", () -> MolangUtils.booleanToFloat(player.isWet()));
        parser.setValue(
            "query.is_jumping",
            () -> MolangUtils.booleanToFloat(
                !player.capabilities.isFlying && !player.isRiding() && !player.onGround && !player.isInWater()));
        parser.setValue("query.is_on_fire", () -> MolangUtils.booleanToFloat(player.isBurning()));
        parser.setValue("query.is_on_ground", () -> MolangUtils.booleanToFloat(player.onGround));
        parser.setValue("query.is_on_ground", () -> MolangUtils.booleanToFloat(player.onGround));
        parser.setValue("query.is_playing_dead", () -> MolangUtils.booleanToFloat(player.isDead));
        parser.setValue("query.is_riding", () -> MolangUtils.booleanToFloat(player.isRiding()));
        parser.setValue("query.is_sleeping", () -> MolangUtils.booleanToFloat(player.isPlayerSleeping()));
        parser.setValue("query.is_sneaking", () -> MolangUtils.booleanToFloat(player.onGround && player.isSneaking()));
        parser.setValue("query.is_spectator", () -> MolangUtils.booleanToFloat(EtfuturumCompat.isSpectator(player)));
        parser.setValue("query.is_sprinting", () -> MolangUtils.booleanToFloat(player.isSprinting()));
        parser.setValue("query.is_swimming", () -> MolangUtils.booleanToFloat(player.isInWater()));
        parser.setValue("query.is_using_item", () -> MolangUtils.booleanToFloat(player.isUsingItem()));
        // In 1.7.10, item use ticks count down. Modern versions count up. The logic is inverted.
        parser.setValue(
            "query.item_in_use_duration",
            () -> (getMaxUseDuration(player) - player.getItemInUseCount()) / 20.0);
        parser.setValue("query.item_max_use_duration", () -> getMaxUseDuration(player) / 20.0);
        parser.setValue("query.item_remaining_use_duration", () -> player.getItemInUseCount() / 20.0);

        parser.setValue("query.max_health", player::getMaxHealth);
        parser.setValue("query.modified_distance_moved", () -> player.distanceWalkedModified);
        parser.setValue("query.moon_phase", () -> mc.theWorld.getMoonPhase());

        parser.setValue("query.player_level", () -> player.experienceLevel);
        parser.setValue("query.time_of_day", () -> MolangUtils.normalizeTime(mc.theWorld.getWorldTime()));
        parser.setValue("query.time_stamp", () -> mc.theWorld.getWorldTime());
        parser.setValue("query.vertical_speed", () -> getVerticalSpeed(player));
        parser.setValue("query.walk_distance", () -> player.distanceWalkedOnStepModified);
        parser.setValue("query.yaw_speed", () -> getYawSpeed(animationEvent, player));

        parser.setValue("ysm.head_yaw", () -> data.netHeadYaw);
        parser.setValue("ysm.head_pitch", () -> data.headPitch);

        parser.setValue("ysm.has_helmet", () -> getSlotValue(player, 4));
        parser.setValue("ysm.has_chest_plate", () -> getSlotValue(player, 3));
        parser.setValue("ysm.has_leggings", () -> getSlotValue(player, 2));
        parser.setValue("ysm.has_boots", () -> getSlotValue(player, 1));
        parser.setValue("ysm.has_mainhand", () -> getSlotValue(player, 0));
        parser.setValue("ysm.has_offhand", () -> getSlotValue(player, 5));
        parser.setValue("ysm.has_elytra", () -> MolangUtils.booleanToFloat(EtfuturumCompat.hasElytra(player)));
        parser.setValue("ysm.elytra_rot_x", () -> EtfuturumCompat.getElytraRot(player, "x"));
        parser.setValue("ysm.elytra_rot_y", () -> EtfuturumCompat.getElytraRot(player, "y"));
        parser.setValue("ysm.elytra_rot_z", () -> EtfuturumCompat.getElytraRot(player, "z"));

        parser.setValue("ysm.is_close_eyes", () -> getEyeCloseState(animationEvent, player));
        parser.setValue("ysm.is_passenger", () -> MolangUtils.booleanToFloat(player.isRiding()));
        parser.setValue("ysm.is_sleep", () -> MolangUtils.booleanToFloat(player.isPlayerSleeping()));
        parser.setValue("ysm.is_sneak", () -> MolangUtils.booleanToFloat(player.onGround && player.isSneaking()));
        // parser.setValue("ysm.is_riptide", () -> MolangUtils.booleanToFloat(player.isAutoSpinAttack()));

        parser.setValue("ysm.armor_value", player::getTotalArmorValue);
        parser.setValue("ysm.hurt_time", () -> player.hurtTime);
        parser.setValue(
            "ysm.food_level",
            () -> player.getFoodStats()
                .getFoodLevel());

    }

    private static boolean hasCape(EntityPlayer player) {
        if (player instanceof AbstractClientPlayer) {
            AbstractClientPlayer clientPlayer = (AbstractClientPlayer) player;
            // 'isCapeLoaded' & 'isModelPartShown' are modern. 1.7.10 has simpler checks.
            // func_152122_n() checks if the cape texture is available and should be rendered.
            return !player.isInvisible() && clientPlayer.func_152122_n() && clientPlayer.getLocationCape() != null;
        }
        return false;
    }

    private static int getEquipmentCount(EntityPlayer player) {
        int count = 0;
        for (ItemStack s : player.inventory.armorInventory) {
            if (s != null) {
                count += 1;
            }
        }
        return count;
    }

    private static float getMaxUseDuration(EntityPlayer player) {
        ItemStack useItem = player.getItemInUse();
        if (useItem == null) {
            return 0.0f;
        } else {
            return useItem.getMaxItemUseDuration();
        }
    }

    private static float getYawSpeed(AnimationEvent<CustomPlayerEntity> animationEvent, EntityPlayer player) {
        return player.rotationYaw - player.prevRotationYaw;
    }

    private static float getGroundSpeed(EntityPlayer player) {
        double dx = player.motionX;
        double dz = player.motionZ;
        return (float) (MathHelper.sqrt_double(dx * dx + dz * dz) * 20.0);
    }

    private static float getVerticalSpeed(EntityPlayer player) {
        return (float) ((player.posY - player.prevPosY) * 20.0);
    }

    private static void register(String animationName, ILoopType loopType, int priority,
        BiPredicate<EntityPlayer, AnimationEvent<CustomPlayerEntity>> predicate) {
        AnimationManager manager = AnimationManager.getInstance();
        manager.register(new AnimationState(animationName, loopType, priority, predicate));
    }

    private static void register(String animationName, int priority,
        BiPredicate<EntityPlayer, AnimationEvent<CustomPlayerEntity>> predicate) {
        register(animationName, ILoopType.EDefaultLoopTypes.LOOP, priority, predicate);
    }

    private static double getEyeCloseState(AnimationEvent<CustomPlayerEntity> animationEvent, EntityPlayer player) {
        double remainder = (animationEvent.getAnimationTick() + Math.abs(
            player.getUniqueID()
                .getLeastSignificantBits())
            % 10) % 90;
        boolean isBlinkTime = 85 < remainder && remainder < 90;
        return MolangUtils.booleanToFloat(player.isPlayerSleeping() || isBlinkTime);
    }

    private static double getSlotValue(EntityPlayer player, int slotIndex) {
        if (slotIndex == 5) {
            return MolangUtils.booleanToFloat(BackhandCompat.getOffhandItem(player) != null);
        } else {
            return MolangUtils.booleanToFloat(player.getEquipmentInSlot(slotIndex) != null);
        }
    }
}
