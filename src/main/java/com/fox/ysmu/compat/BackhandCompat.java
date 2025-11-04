package com.fox.ysmu.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.Loader;
import xonin.backhand.api.core.BackhandUtils;

import javax.annotation.Nullable;

public class BackhandCompat {
    private static final boolean BACKHAND_LOADED = Loader.isModLoaded("backhand");

    /**
     * 检查Backhand mod是否已加载
     * @return 如果Backhand mod已加载则返回true，否则返回false
     */
    public static boolean isBackhandLoaded() {
        return BACKHAND_LOADED;
    }

    /**
     * 获取玩家副手物品
     * @param player 玩家实体
     * @return 如果加载了Backhand则返回副手物品，否则返回null
     */
    public static @Nullable ItemStack getOffhandItem(EntityPlayer player) {
        if (BACKHAND_LOADED) {
            return BackhandUtils.getOffhandItem(player);
        }
        return null;
    }

    public static void setOffhandItem(EntityPlayer player, @Nullable ItemStack itemStack) {
        if (BACKHAND_LOADED) {
            BackhandUtils.setPlayerOffhandItem(player, itemStack);
        }
    }

    /**
     * 获取指定手的物品
     * @param player 玩家实体
     * @param isMainHand 是否为主手
     * @return 对应手的物品
     */
    public static ItemStack getItemInHand(EntityPlayer player, boolean isMainHand) {
        if (BACKHAND_LOADED) {
            if (isMainHand) {
                return player.getHeldItem();
            } else {
                return getOffhandItem(player);
            }
        } else {
            return player.getHeldItem();
        }
    }

    public static boolean swingingArm(EntityPlayer player) {
        if (BACKHAND_LOADED) {
            return !BackhandUtils.isUsingOffhand(player); // true表示主手
        }
        return true;
    }
    public static boolean getUsedItemHand(EntityPlayer player) {
        return swingingArm(player);
    }
}
