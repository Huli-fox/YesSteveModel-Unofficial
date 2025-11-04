package com.fox.ysmu.client.animation.condition;

import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.google.common.collect.Maps;

public class ConditionManager {

    public static Map<ResourceLocation, ConditionalSwing> SWING = Maps.newHashMap();
    public static Map<ResourceLocation, ConditionalUse> USE_MAINHAND = Maps.newHashMap();
    public static Map<ResourceLocation, ConditionalUse> USE_OFFHAND = Maps.newHashMap();
    public static Map<ResourceLocation, ConditionalHold> HOLD_MAINHAND = Maps.newHashMap();
    public static Map<ResourceLocation, ConditionalHold> HOLD_OFFHAND = Maps.newHashMap();
    public static Map<ResourceLocation, ConditionArmor> ARMOR = Maps.newHashMap();

    public static void addTest(ResourceLocation id, String name) {
        SWING.putIfAbsent(id, new ConditionalSwing());
        USE_MAINHAND.putIfAbsent(id, new ConditionalUse(true)); // true表示主手
        USE_OFFHAND.putIfAbsent(id, new ConditionalUse(false)); // false表示副手
        HOLD_MAINHAND.putIfAbsent(id, new ConditionalHold(true));
        HOLD_OFFHAND.putIfAbsent(id, new ConditionalHold(false));
        ARMOR.putIfAbsent(id, new ConditionArmor());

        ConditionalSwing conditionalSwing = SWING.get(id);
        ConditionalUse conditionalUseMainhand = USE_MAINHAND.get(id);
        ConditionalUse conditionalUseOffhand = USE_OFFHAND.get(id);
        ConditionalHold conditionalHoldMainhand = HOLD_MAINHAND.get(id);
        ConditionalHold conditionalHoldOffhand = HOLD_OFFHAND.get(id);
        ConditionArmor conditionArmor = ARMOR.get(id);

        conditionalSwing.addTest(name);
        conditionalUseMainhand.addTest(name);
        conditionalUseOffhand.addTest(name);
        conditionalHoldMainhand.addTest(name);
        conditionalHoldOffhand.addTest(name);
        conditionArmor.addTest(name);
    }

    public static void clear() {
        SWING.clear();
        USE_MAINHAND.clear();
        USE_OFFHAND.clear();
        HOLD_MAINHAND.clear();
        HOLD_OFFHAND.clear();
        ARMOR.clear();
    }

    public static ConditionalSwing getSwing(ResourceLocation id) {
        return SWING.get(id);
    }

    public static ConditionalUse getUseMainhand(ResourceLocation id) {
        return USE_MAINHAND.get(id);
    }

    public static ConditionalUse getUseOffhand(ResourceLocation id) {
        return USE_OFFHAND.get(id);
    }

    public static ConditionalHold getHoldMainhand(ResourceLocation id) {
        return HOLD_MAINHAND.get(id);
    }

    public static ConditionalHold getHoldOffhand(ResourceLocation id) {
        return HOLD_OFFHAND.get(id);
    }

    public static ConditionArmor getArmor(ResourceLocation id) {
        return ARMOR.get(id);
    }
}
