package com.fox.ysmu.client.gui;

import com.fox.ysmu.client.ClientEventHandler;
import com.fox.ysmu.client.input.DebugAnimationKey;
import software.bernie.geckolib3.util.MolangUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Math;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.ModList;

import java.util.Locale;
import java.util.function.DoubleSupplier;

public class DebugAnimationScreen implements IGuiOverlay {
    public static final String FIRST_PERSON_MOD_ID = "firstpersonmod";

    @Override
    public void drawScreen(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!DebugAnimationKey.DEBUG) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.renderDebug) {
            return;
        }

        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) {
            return;
        }

        float lerpBodyRot = Math.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float lerpHeadRot = Math.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float netHeadYaw = lerpHeadRot - lerpBodyRot;
        boolean shouldSit = player.isPassenger() && (player.getVehicle() != null && player.getVehicle().shouldRiderSit());

        if (shouldSit && player.getVehicle() instanceof LivingEntity vehicle) {
            lerpBodyRot = Math.rotLerp(partialTick, vehicle.yBodyRotO, vehicle.yBodyRot);
            netHeadYaw = lerpHeadRot - lerpBodyRot;
            float clampedHeadYaw = Math.clamp(Math.wrapDegrees(netHeadYaw), -85, 85);
            lerpBodyRot = lerpHeadRot - clampedHeadYaw;
            if (clampedHeadYaw * clampedHeadYaw > 2500f) {
                lerpBodyRot += clampedHeadYaw * 0.2f;
            }
            netHeadYaw = lerpHeadRot - lerpBodyRot;
        }
        float headPitch = Math.lerp(partialTick, player.xRotO, player.getXRot());
        final float outputHeadPitch = -headPitch;
        final float outputNetHeadYaw = -netHeadYaw;

        int[] y = {5};

        renderText(gui, graphics, y, "PI", String.format("§7%.4f", Math.PI));
        renderText(gui, graphics, y, "E", String.format("§7%.4f", Math.E));

        renderText(gui, graphics, y, "query.actor_count", mc.level.getEntityCount());
        renderText(gui, graphics, y, "query.anim_time", () -> 0.0);

        renderText(gui, graphics, y, "query.body_x_rotation", player::getXRot);
        renderText(gui, graphics, y, "query.body_y_rotation", () -> Math.wrapDegrees(player.getYRot()));
        renderText(gui, graphics, y, "query.cardinal_facing_2d", player.getDirection().get3DDataValue());
        renderText(gui, graphics, y, "query.distance_from_camera", () -> mc.gameRenderer.getMainCamera().getPosition().distanceTo(player.position()));
        renderText(gui, graphics, y, "query.equipment_count", getEquipmentCount(player));
        renderText(gui, graphics, y, "query.eye_target_x_rotation", () -> player.getViewXRot(partialTick));
        renderText(gui, graphics, y, "query.eye_target_y_rotation", () -> player.getViewYRot(partialTick));
        renderText(gui, graphics, y, "query.ground_speed", () -> getGroundSpeed(player));

        renderText(gui, graphics, y, "query.has_cape", hasCape(player));
        renderText(gui, graphics, y, "query.has_rider", player.isVehicle());
        renderText(gui, graphics, y, "query.head_x_rotation", () -> outputNetHeadYaw);
        renderText(gui, graphics, y, "query.head_y_rotation", () -> outputHeadPitch);
        renderText(gui, graphics, y, "query.health", player::getHealth);
        renderText(gui, graphics, y, "query.hurt_time", player.hurtTime);

        renderText(gui, graphics, y, "query.is_eating", player.getUseItem().getUseAnimation() == UseAnim.EAT);
        renderText(gui, graphics, y, "query.is_first_person", mc.options.getCameraType() == CameraType.FIRST_PERSON);
        renderText(gui, graphics, y, "query.is_in_water", player.isInWater());
        renderText(gui, graphics, y, "query.is_in_water_or_rain", player.isInWaterRainOrBubble());
        renderText(gui, graphics, y, "query.is_jumping", !player.getAbilities().flying && !player.isPassenger() && !player.onGround() && !player.isInWater());
        renderText(gui, graphics, y, "query.is_on_fire", player.isOnFire());
        renderText(gui, graphics, y, "query.is_on_ground", player.onGround());
        renderText(gui, graphics, y, "query.is_playing_dead", player.isDeadOrDying());
        renderText(gui, graphics, y, "query.is_riding", player.isPassenger());
        renderText(gui, graphics, y, "query.is_sleeping", player.isSleeping());
        renderText(gui, graphics, y, "query.is_sneaking", player.onGround() && player.getPose() == Pose.CROUCHING);
        renderText(gui, graphics, y, "query.is_spectator", player.isSpectator());
        renderText(gui, graphics, y, "query.is_sprinting", player.isSprinting());
        renderText(gui, graphics, y, "query.is_swimming", player.isSwimming());
        renderText(gui, graphics, y, "query.is_using_item", player.isUsingItem());
        renderText(gui, graphics, y, "query.item_in_use_duration", () -> player.getTicksUsingItem() / 20.0);
        renderText(gui, graphics, y, "query.item_max_use_duration", () -> getMaxUseDuration(player) / 20.0);
        renderText(gui, graphics, y, "query.item_remaining_use_duration", () -> player.getUseItemRemainingTicks() / 20.0);

        renderText(gui, graphics, y, "query.life_time", () -> (player.tickCount + partialTick) / 20.0);
        renderText(gui, graphics, y, "query.max_health", player::getMaxHealth);
        renderText(gui, graphics, y, "query.modified_distance_moved", () -> player.walkDist);
        renderText(gui, graphics, y, "query.moon_phase", mc.level.getMoonPhase());

        renderText(gui, graphics, y, "query.player_level", player.experienceLevel);
        renderText(gui, graphics, y, "query.time_of_day", () -> MolangUtils.normalizeTime(mc.level.getDayTime()));
        renderText(gui, graphics, y, "query.time_stamp", mc.level.getDayTime());
        renderText(gui, graphics, y, "query.vertical_speed", () -> getVerticalSpeed(player));
        renderText(gui, graphics, y, "query.walk_distance", () -> player.moveDist);
        renderText(gui, graphics, y, "query.yaw_speed", () -> getYawSpeed(partialTick, player));

        renderText(gui, graphics, y, "ysm.armor_value", player.getArmorValue());

        renderText(gui, graphics, y, "ysm.has_helmet", getSlotValue(player, EquipmentSlot.HEAD));
        renderText(gui, graphics, y, "ysm.has_chest_plate", getSlotValue(player, EquipmentSlot.CHEST));
        renderText(gui, graphics, y, "ysm.has_leggings", getSlotValue(player, EquipmentSlot.LEGS));
        renderText(gui, graphics, y, "ysm.has_boots", getSlotValue(player, EquipmentSlot.FEET));
        renderText(gui, graphics, y, "ysm.has_mainhand", getSlotValue(player, EquipmentSlot.MAINHAND));
        renderText(gui, graphics, y, "ysm.has_offhand", getSlotValue(player, EquipmentSlot.OFFHAND));

        renderText(gui, graphics, y, "ysm.has_elytra", player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA);
        renderText(gui, graphics, y, "ysm.elytra_rot_x", () -> Math.toDegrees(player.elytraRotX));
        renderText(gui, graphics, y, "ysm.elytra_rot_y", () -> Math.toDegrees(player.elytraRotY));
        renderText(gui, graphics, y, "ysm.elytra_rot_z", () -> Math.toDegrees(player.elytraRotZ));

        renderText(gui, graphics, y, "ysm.is_close_eyes", getEyeCloseState(partialTick, player));
        renderText(gui, graphics, y, "ysm.is_riptide", player.isAutoSpinAttack());
    }

    private static boolean hasCape(Player player) {
        if (player instanceof AbstractClientPlayer clientPlayer) {
            return clientPlayer.isCapeLoaded() && !player.isInvisible() && clientPlayer.isModelPartShown(PlayerModelPart.CAPE) && clientPlayer.getCloakTextureLocation() != null;
        }
        return false;
    }

    private static int getEquipmentCount(Player player) {
        int count = 0;
        for (ItemStack s : player.getArmorSlots()) {
            if (!s.isEmpty()) {
                count += 1;
            }
        }
        return count;
    }

    private static double getMaxUseDuration(Player player) {
        ItemStack useItem = player.getUseItem();
        if (useItem.isEmpty()) {
            return 0.0;
        } else {
            return useItem.getUseDuration();
        }
    }

    private static void renderText(ForgeGui gui, GuiGraphics graphics, int[] y, String name, String data) {
        Font font = gui.getFont();
        String s = I18n.get("molang.yes_steve_model.bg_width");
        if ((y[0] - 5) % 20 == 0) {
            graphics.fill(2, y[0] - 1, ClientEventHandler.DEBUG_BG_WIDTH, y[0] + 9, 0xc0505050);
        } else {
            graphics.fill(2, y[0] - 1, ClientEventHandler.DEBUG_BG_WIDTH, y[0] + 9, 0xc0506050);
        }
        graphics.drawString(font, name, 5, y[0], 0xffffff);
        graphics.drawString(font, data, 200, y[0], 0xffffff);
        graphics.drawString(font, Component.translatable(String.format("molang.yes_steve_model.%s.desc", name.toLowerCase(Locale.US))), 260, y[0], ChatFormatting.GRAY.getColor());
        y[0] = y[0] + 10;
    }

    private static void renderText(ForgeGui gui, GuiGraphics graphics, int[] y, String name, DoubleSupplier supplier) {
        renderText(gui, graphics, y, name, String.format("§b%.4f", supplier.getAsDouble()));
    }

    private static void renderText(ForgeGui gui, GuiGraphics graphics, int[] y, String name, int number) {
        renderText(gui, graphics, y, name, String.format("§6%d", number));
    }

    private static void renderText(ForgeGui gui, GuiGraphics graphics, int[] y, String name, long number) {
        renderText(gui, graphics, y, name, String.format("§1%d", number));
    }

    private static void renderText(ForgeGui gui, GuiGraphics graphics, int[] y, String name, boolean data) {
        String str = data ? String.format("§c%b", data) : String.format("§a%b", data);
        renderText(gui, graphics, y, name, str);
    }

    private static float getYawSpeed(float partialTick, Player player) {
        double seekTime = player.tickCount + partialTick;
        return player.getViewYRot((float) seekTime - player.getViewYRot((float) seekTime - 0.1f));
    }

    private static float getGroundSpeed(Player player) {
        Vec3 velocity = player.getDeltaMovement();
        return 20 * Math.sqrt((float) ((velocity.x * velocity.x) + (velocity.z * velocity.z)));
    }

    private static float getVerticalSpeed(Player player) {
        return 20 * (float) (player.position().y - player.yo);
    }

    private static boolean getEyeCloseState(float partialTick, Player player) {
        double remainder = (player.tickCount + partialTick + Math.abs(player.getUUID().getLeastSignificantBits()) % 10) % 90;
        boolean isBlinkTime = 85 < remainder && remainder < 90;
        return player.isSleeping() || isBlinkTime;
    }

    private static boolean getSlotValue(Player player, EquipmentSlot slot) {
        return !player.getItemBySlot(slot).isEmpty();
    }
}
