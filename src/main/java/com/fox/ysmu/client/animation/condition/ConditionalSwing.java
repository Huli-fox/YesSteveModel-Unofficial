package com.fox.ysmu.client.animation.condition;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.fox.ysmu.compat.BackhandCompat;
import com.google.common.collect.Lists;

import cpw.mods.fml.common.registry.GameRegistry;

public class ConditionalSwing {

    private static final String ID_PRE = "swing$";
    private static final String OD_PRE = "swing#";
    private static final String EMPTY = "";
    private static final int PRE_SIZE = 6;
    // 1.7.10: 使用String存储物品ID ("modid:name")
    private final List<String> idTest = Lists.newArrayList();
    // 1.7.10: 使用String存储矿物词典名称
    private final List<String> oreDictTest = Lists.newArrayList();

    public void addTest(String name) {
        if (name.length() <= PRE_SIZE) {
            return;
        }
        String substring = name.substring(PRE_SIZE);
        if (name.startsWith(ID_PRE)) {
            // 1.7.10: 简单验证格式即可，不再有 isValidResourceLocation 方法
            if (substring.contains(":")) {
                idTest.add(substring);
            }
        }
        if (name.startsWith(OD_PRE)) {
            // 1.7.10: 这里处理的是矿物词典名称
            oreDictTest.add(substring);
        }
    }

    public String doTest(EntityPlayer player, boolean isMainHand) {
        if (BackhandCompat.getItemInHand(player, isMainHand) == null) {
            return EMPTY;
        }
        String result = doIdTest(player, isMainHand);
        if (result.isEmpty()) {
            return doOreDictTest(player, isMainHand);
        }
        return result;
    }

    private String doIdTest(EntityPlayer player, boolean isMainHand) {
        if (idTest.isEmpty()) {
            return EMPTY;
        }
        ItemStack itemInHand = BackhandCompat.getItemInHand(player, isMainHand);
        // 1.7.10: 使用 GameRegistry 获取物品的唯一标识符
        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(itemInHand.getItem());
        if (uid == null) {
            return EMPTY;
        }
        String registryName = uid.toString(); // 格式为 "modid:name"
        if (idTest.contains(registryName)) {
            return ID_PRE + registryName;
        }
        return EMPTY;
    }

    private String doOreDictTest(EntityPlayer player, boolean isMainHand) {
        if (oreDictTest.isEmpty()) {
            return EMPTY;
        }
        ItemStack itemInHand = BackhandCompat.getItemInHand(player, isMainHand);
        // 获取物品堆栈对应的所有矿辞ID
        int[] oreIDs = OreDictionary.getOreIDs(itemInHand);
        if (oreIDs.length == 0) {
            return EMPTY;
        }

        // 遍历物品拥有的所有矿辞
        for (int oreID : oreIDs) {
            String oreName = OreDictionary.getOreName(oreID);
            // 检查这个矿辞名称是否在我们需要测试的列表里
            if (oreDictTest.contains(oreName)) {
                return OD_PRE + oreName; // 找到匹配，返回结果
            }
        }

        return EMPTY; // 未找到匹配
    }
}
