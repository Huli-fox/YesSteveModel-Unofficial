// TODO 待检查
package com.fox.ysmu.client.animation.condition;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionArmor {
    private static final Pattern ID_PRE_REG = Pattern.compile("^(.+?)\\$(.*?)$");
    private static final Pattern TAG_PRE_REG = Pattern.compile("^(.+?)#(.*?)$");
    private static final String EMPTY = "";

    // 1.7.10: 使用 Integer 作为 Key 来代表装备槽位索引
    private final Map<Integer, List<String>> idTest = Maps.newHashMap();
    private final Map<Integer, List<String>> oreDictTest = Maps.newHashMap();

    public void addTest(String name) {
        Matcher matcherId = ID_PRE_REG.matcher(name);
        if (matcherId.find()) {
            // 1.7.10: 将字符串槽位名转换为整数索引
            int slotIndex = getSlotIndexFromString(matcherId.group(1));
            if (slotIndex == -1) { // -1 表示无效槽位
                return;
            }
            String id = matcherId.group(2);
            // 1.7.10: 简单验证ID格式
            if (!id.contains(":")) {
                return;
            }
            if (idTest.containsKey(slotIndex)) {
                idTest.get(slotIndex).add(id);
            } else {
                idTest.put(slotIndex, Lists.newArrayList(id));
            }
            return;
        }

        Matcher matcherTag = TAG_PRE_REG.matcher(name);
        if (matcherTag.find()) {
            int slotIndex = getSlotIndexFromString(matcherTag.group(1));
            if (slotIndex == -1) {
                return;
            }
            String oreName = matcherTag.group(2);
            if (oreDictTest.containsKey(slotIndex)) {
                oreDictTest.get(slotIndex).add(oreName);
            } else {
                oreDictTest.put(slotIndex, Lists.newArrayList(oreName));
            }
        }
    }

    // 1.7.10: 方法签名改变，使用 EntityPlayer 和 int 索引
    public String doTest(EntityPlayer player, int slotIndex) {
        // 1.7.10: getEquipmentInSlot(0) 是手持，1-4是盔甲。
        // 但玩家的盔甲数组是0-3，getEquipmentInSlot 会自动处理。
        // 为了匹配原来的逻辑，我们需要一个映射。
        // getArmorItemInSlot(index) -> index: 0=feet, 1=legs, 2=chest, 3=head
        // getEquipmentInSlot(index) -> index: 1=feet, 2=legs, 3=chest, 4=head
        // 我们在 getSlotIndexFromString 中统一使用 getEquipmentInSlot 的索引
        ItemStack item = player.getEquipmentInSlot(slotIndex);
        if (item == null) { // 1.7.10: 用 null 检查
            return EMPTY;
        }
        String result = doIdTest(player, slotIndex);
        if (result.isEmpty()) {
            return doOreDictTest(player, slotIndex);
        }
        return result;
    }

    private String doIdTest(EntityPlayer player, int slotIndex) {
        if (idTest.isEmpty() || !idTest.containsKey(slotIndex) || idTest.get(slotIndex).isEmpty()) {
            return EMPTY;
        }
        List<String> idListTest = idTest.get(slotIndex);
        ItemStack item = player.getEquipmentInSlot(slotIndex);
        if (item == null) return EMPTY; // 安全检查

        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(item.getItem());
        if (uid == null) {
            return EMPTY;
        }
        String registryName = uid.toString();
        if (idListTest.contains(registryName)) {
            // 1.7.10: 将索引转换回字符串名
            return getSlotNameFromIndex(slotIndex) + "$" + registryName;
        }
        return EMPTY;
    }

    private String doOreDictTest(EntityPlayer player, int slotIndex) {
        if (oreDictTest.isEmpty() || !oreDictTest.containsKey(slotIndex) || oreDictTest.get(slotIndex).isEmpty()) {
            return EMPTY;
        }
        List<String> oreDictListTest = oreDictTest.get(slotIndex);
        ItemStack item = player.getEquipmentInSlot(slotIndex);
        if (item == null) return EMPTY; // 安全检查

        int[] oreIDs = OreDictionary.getOreIDs(item);
        if (oreIDs.length == 0) {
            return EMPTY;
        }

        for (int oreID : oreIDs) {
            String oreName = OreDictionary.getOreName(oreID);
            if (oreDictListTest.contains(oreName)) {
                return getSlotNameFromIndex(slotIndex) + "#" + oreName;
            }
        }
        return EMPTY;
    }

    /**
     * 1.7.10 Helper: 将高版本的槽位字符串名转换为1.7.10的整数索引。
     * 使用 EntityPlayer.getEquipmentInSlot(int) 的索引。
     * @param type "head", "chest", "legs", "feet"
     * @return 1-4 for armor, -1 for invalid
     */
    public static int getSlotIndexFromString(String type) {
        if ("head".equals(type)) {
            return 4;
        }
        if ("chest".equals(type)) {
            return 3;
        }
        if ("legs".equals(type)) {
            return 2;
        }
        if ("feet".equals(type)) {
            return 1;
        }
        return -1; // 表示无效或不支持的槽位
    }

    /**
     * 1.7.10 Helper: 将整数索引转换回高版本的槽位字符串名。
     * @param index 1-4
     * @return "head", "chest", "legs", "feet", or ""
     */
    public static String getSlotNameFromIndex(int index) {
        switch (index) {
            case 1:
                return "feet";
            case 2:
                return "legs";
            case 3:
                return "chest";
            case 4:
                return "head";
            default:
                return "";
        }
    }
}
