package com.fox.ysmu.client.animation.condition;

import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.oredict.OreDictionary;

import com.fox.ysmu.compat.BackhandCompat;

import cpw.mods.fml.common.registry.GameRegistry;

public final class InnerClassify {

    private InnerClassify() {}

    static String doClassifyTest(String prefix, EntityPlayer player, boolean isMainHand) {
        String itemType = getItemType(BackhandCompat.getItemInHand(player, isMainHand));
        return itemType.isEmpty() ? "" : prefix + itemType;
    }

    public static String getItemType(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "";
        }
        Item item = stack.getItem();
        if (item instanceof ItemSword || matchesName(stack, "sword")) {
            return "sword";
        }
        if (item instanceof ItemPickaxe || matchesName(stack, "pickaxe")) {
            return "pickaxe";
        }
        if (item instanceof ItemSpade || matchesName(stack, "shovel") || matchesName(stack, "spade")) {
            return "shovel";
        }
        if (item instanceof ItemHoe || matchesName(stack, "hoe")) {
            return "hoe";
        }
        if (item instanceof ItemAxe || matchesName(stack, "axe")) {
            return "axe";
        }
        if (matchesName(stack, "shield")) {
            return "shield";
        }
        if (matchesName(stack, "crossbow")) {
            return "crossbow";
        }
        if (item instanceof ItemBow || matchesName(stack, "bow")) {
            return "bow";
        }
        if (item instanceof ItemFishingRod || matchesName(stack, "fishing_rod") || matchesName(stack, "fishingrod")) {
            return "fishing_rod";
        }
        if (matchesName(stack, "spear") || matchesName(stack, "trident")) {
            return "spear";
        }
        if ((item instanceof ItemPotion && ItemPotion.isSplash(stack.getItemDamage()))
            || matchesName(stack, "throwable_potion")) {
            return "throwable_potion";
        }
        return "";
    }

    private static boolean matchesName(ItemStack stack, String needle) {
        String normalizedNeedle = needle.toLowerCase(Locale.ROOT);
        String itemId = itemId(stack);
        if (itemId.contains(normalizedNeedle)) {
            return true;
        }
        int[] oreIds = OreDictionary.getOreIDs(stack);
        for (int oreId : oreIds) {
            String oreName = OreDictionary.getOreName(oreId);
            if (oreName != null && oreName.toLowerCase(Locale.ROOT).contains(normalizedNeedle)) {
                return true;
            }
        }
        return false;
    }

    private static String itemId(ItemStack stack) {
        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        return uid == null ? "" : uid.toString().toLowerCase(Locale.ROOT);
    }
}
