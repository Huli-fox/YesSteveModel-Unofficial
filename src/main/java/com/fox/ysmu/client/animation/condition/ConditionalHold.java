package com.fox.ysmu.client.animation.condition;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.fox.ysmu.compat.BackhandCompat;
import com.google.common.collect.Lists;

import cpw.mods.fml.common.registry.GameRegistry;

public class ConditionalHold {

    // 在 YesSteveModel 项目中，这些 Minecraft 标签文件（swords.json, axes.json, pickaxes.json, shovels.json, hoes.json,
    // tools.json）主要用于动画系统的条件判断。
    // 1.动画条件系统：
    // ·项目中的 ConditionalSwing、ConditionalUse 和 ConditionalHold 类用于根据玩家手持物品的类型来触发特定动画。
    // ·这些类通过 doTagTest 方法检查玩家手中的物品是否属于某个标签，例如检查是否是剑、斧头、镐等工具。
    // 2.工作原理：
    // ·当玩家挥动物品时，系统会检查该物品是否属于 swords 标签，如果是，则可能触发特定的剑类挥动动画。
    // ·当玩家使用物品时，系统会检查该物品是否属于 tools 标签，如果是，则可能触发工具使用动画。
    // ·这种机制允许为不同类型的物品定义不同的动画，而不需要为每种具体物品硬编码。
    // 3.实际应用示例：
    // ·当玩家手持任何类型的剑（通过 swords.json 标签定义）时，可能会触发特定的剑类攻击动画。
    // ·当玩家手持任何类型的镐（通过 pickaxes.json 标签定义）时，可能会触发特定的挖掘动画。
    // ·通过使用这些标签，项目可以更灵活地管理动画系统，而不需要为每种具体物品单独编写代码。当 Minecraft 添加新物品或模组添加新物品时，只要它们正确地添加到相应的标签中，动画系统就会自动支持它们。
    private static final String EMPTY = "";
    private final int preSize;
    private final String idPre;
    private final String oreDictPre; // 在1.7.10中，我们用它来表示矿物词典的前缀
    // 1.7.10: 不再使用 ResourceLocation，直接用 String 存储物品ID ("modid:name")
    private final List<String> idTest = Lists.newArrayList();
    // 1.7.10: 不再使用 TagKey，直接用 String 存储矿物词典的名称
    private final List<String> oreDictTest = Lists.newArrayList();

    public ConditionalHold(boolean isMainHand) {
        if (isMainHand) {
            idPre = "hold_mainhand$";
            oreDictPre = "hold_mainhand#";
            preSize = 14;
        } else {
            idPre = "hold_offhand$";
            oreDictPre = "hold_offhand#";
            preSize = 13;
        }
    }

    public void addTest(String name) {
        if (name.length() <= preSize) {
            return;
        }
        String substring = name.substring(preSize);
        if (name.startsWith(idPre)) {
            // 1.7.10: 简单验证格式即可，不再有 isValidResourceLocation 方法
            if (substring.contains(":")) {
                idTest.add(substring);
            }
        }
        if (name.startsWith(oreDictPre)) {
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
            return idPre + registryName;
        }
        return EMPTY;
    }

    // 1.7.10: doTagTest 完全重写，使用 OreDictionary
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
                return oreDictPre + oreName; // 找到匹配，返回结果
            }
        }

        return EMPTY; // 未找到匹配
    }
}
