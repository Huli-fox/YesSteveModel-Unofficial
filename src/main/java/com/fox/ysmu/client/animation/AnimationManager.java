package com.fox.ysmu.client.animation;

import java.util.LinkedList;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.fox.ysmu.client.animation.condition.*;
import com.fox.ysmu.client.entity.CustomPlayerEntity;
import com.fox.ysmu.compat.BackhandCompat;
import com.fox.ysmu.eep.ExtendedModelInfo;
import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.resource.GeckoLibCache;

public final class AnimationManager {

    private static AnimationManager MANAGER;
    private final Int2ObjectOpenHashMap<LinkedList<AnimationState>> data = new Int2ObjectOpenHashMap<>();

    public static AnimationManager getInstance() {
        if (MANAGER == null) {
            MANAGER = new AnimationManager();
        }
        return MANAGER;
    }

    @NotNull
    private static <P extends IAnimatable> PlayState playLoopAnimation(AnimationEvent<P> event, String animationName) {
        return playAnimation(event, animationName, ILoopType.EDefaultLoopTypes.LOOP);
    }

    @NotNull
    private static <P extends IAnimatable> PlayState playAnimation(AnimationEvent<P> event, String animationName,
        ILoopType loopType) {
        event.getController()
            .setAnimation(new AnimationBuilder().addAnimation(animationName, loopType));
        return PlayState.CONTINUE;
    }

    @NotNull
    private static <P extends IAnimatable> PlayState playAnimation(AnimationEvent<P> event, String animationName) {
        event.getController()
            .setAnimation(new AnimationBuilder().addAnimation(animationName));
        return PlayState.CONTINUE;
    }

    public void register(AnimationState state) {
        if (data.containsKey(state.getPriority())) {
            data.get(state.getPriority())
                .add(state);
        } else {
            LinkedList<AnimationState> states = Lists.newLinkedList();
            states.add(state);
            data.put(state.getPriority(), states);
        }
    }

    public PlayState predicateParallel(AnimationEvent<CustomPlayerEntity> event, String animationName) {
        if (Minecraft.getMinecraft()
            .isGamePaused()) {
            return PlayState.STOP;
        }
        return playLoopAnimation(event, animationName);
    }

    public PlayState predicateCap(AnimationEvent<CustomPlayerEntity> event) {
        CustomPlayerEntity animatable = event.getAnimatable();
        EntityPlayer player = animatable.getPlayer();
        if (player == null) {
            if (animatable.hasPreviewAnimation()) {
                return playLoopAnimation(event, animatable.getPreviewAnimation());
            }
            return PlayState.STOP;
        }

        ExtendedModelInfo eep = ExtendedModelInfo.get(player);
        if (eep != null && eep.isPlayAnimation()) {
            return playAnimation(event, eep.getAnimation());
        }
        return PlayState.STOP;
    }

    @NotNull
    public PlayState predicateMain(AnimationEvent<CustomPlayerEntity> event) {
        EntityPlayer player = event.getAnimatable()
            .getPlayer();
        if (player == null) {
            return PlayState.STOP;
        }
        for (int i = Priority.HIGHEST; i <= Priority.LOWEST; i++) {
            if (!data.containsKey(i)) {
                continue;
            }
            LinkedList<AnimationState> states = data.get(i);
            for (AnimationState state : states) {
                if (state.getPredicate()
                    .test(player, event)) {
                    String animationName = state.getAnimationName();
                    ILoopType loopType = state.getLoopType();
                    return playAnimation(event, animationName, loopType);
                }
            }
        }
        return PlayState.STOP;
    }

    public PlayState predicateOffhandHold(AnimationEvent<CustomPlayerEntity> event) {
        EntityPlayer player = event.getAnimatable()
            .getPlayer();
        if (player == null) {
            return PlayState.STOP;
        }

        // 修改为使用BackhandCompat兼容层
        ItemStack offhandItem = BackhandCompat.getOffhandItem(player);
        if (offhandItem != null && checkSwingAndUse(player, false)) {
            ResourceLocation id = event.getAnimatable()
                .getAnimation();
            ConditionalHold conditionalHold = ConditionManager.getHoldOffhand(id);
            if (conditionalHold != null) {
                // 为兼容性传递false表示副手
                String name = conditionalHold.doTest(player, false);
                if (StringUtils.isNoneBlank(name)) {
                    return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                }
            }
        }
        return PlayState.STOP;
    }

    public PlayState predicateMainhandHold(AnimationEvent<CustomPlayerEntity> event) {
        EntityPlayer player = event.getAnimatable()
            .getPlayer();
        if (player == null) {
            return PlayState.STOP;
        }
        if (!player.isSwingInProgress && !player.isUsingItem()) {
            // ItemStack mainHandItem = player.getHeldItem();
            // if (mainHandItem.is(Items.CROSSBOW) && CrossbowItem.isCharged(mainHandItem)) {
            // return playAnimation(event, "hold_mainhand:charged_crossbow", ILoopType.EDefaultLoopTypes.LOOP);
            // }
            // ItemStack offhandItem = BackhandCompat.getOffhandItem(player);
            // if (offhandItem != null && offhandItem.is(Items.CROSSBOW) && CrossbowItem.isCharged(offhandItem)) {
            // return playAnimation(event, "hold_offhand:charged_crossbow", ILoopType.EDefaultLoopTypes.LOOP);
            // }
            if (player.fishEntity != null) {
                return playAnimation(event, "hold_mainhand:fishing", ILoopType.EDefaultLoopTypes.LOOP);
            }
        }

        if (player.getHeldItem() != null && checkSwingAndUse(player, true)) {
            ResourceLocation id = event.getAnimatable()
                .getAnimation();
            ConditionalHold conditionalHold = ConditionManager.getHoldMainhand(id);
            if (conditionalHold != null) {
                String name = conditionalHold.doTest(player, true);
                if (StringUtils.isNoneBlank(name)) {
                    return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                }
            }
        }
        return PlayState.STOP;
    }

    public PlayState predicateSwing(AnimationEvent<CustomPlayerEntity> event) {
        EntityPlayer player = event.getAnimatable()
            .getPlayer();
        if (player == null) {
            return PlayState.STOP;
        }
        if (player.isSwingInProgress && !player.isPlayerSleeping()) {
            if (player.swingProgressInt == 0) {
                event.getController().shouldResetTick = true;
                event.getController()
                    .adjustTick(0);
            }
            ResourceLocation id = event.getAnimatable()
                .getAnimation();
            ConditionalSwing conditionalSwing = ConditionManager.getSwing(id);
            if (conditionalSwing != null) {
                // 修改为使用兼容性方法
                String name = conditionalSwing.doTest(player, BackhandCompat.swingingArm(player));
                if (StringUtils.isNoneBlank(name)) {
                    return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                }
            }
            return playAnimation(event, "swing_hand", ILoopType.EDefaultLoopTypes.LOOP);
        }
        return PlayState.STOP;
    }

    public PlayState predicateUse(AnimationEvent<CustomPlayerEntity> event) {
        EntityPlayer player = event.getAnimatable()
            .getPlayer();
        if (player == null) {
            return PlayState.STOP;
        }
        if (player.isUsingItem() && !player.isPlayerSleeping()) {
            if (player.getItemInUseDuration() == 1) { // TODO getItemInUseCount可能与高版本逻辑相反
                event.getController().shouldResetTick = true;
                event.getController()
                    .adjustTick(0);
            }
            if (BackhandCompat.getUsedItemHand(player)) { // 主手
                ResourceLocation id = event.getAnimatable()
                    .getAnimation();
                ConditionalUse conditionalUse = ConditionManager.getUseMainhand(id);
                if (conditionalUse != null) {
                    String name = conditionalUse.doTest(player, true); // true表示主手
                    if (StringUtils.isNoneBlank(name)) {
                        return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                    }
                }
                return playAnimation(event, "use_mainhand", ILoopType.EDefaultLoopTypes.LOOP);
            } else {
                ResourceLocation id = event.getAnimatable()
                    .getAnimation();
                ConditionalUse conditionalUse = ConditionManager.getUseOffhand(id);
                if (conditionalUse != null) {
                    String name = conditionalUse.doTest(player, false); // false表示副手
                    if (StringUtils.isNoneBlank(name)) {
                        return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                    }
                }
                return playAnimation(event, "use_offhand", ILoopType.EDefaultLoopTypes.LOOP);
            }
        }
        return PlayState.STOP;
    }

    public PlayState predicateArmor(AnimationEvent<CustomPlayerEntity> event, int slotIndex) {
        EntityPlayer player = event.getAnimatable()
            .getPlayer();
        if (player == null) {
            return PlayState.STOP;
        }
        ItemStack itemBySlot = player.getEquipmentInSlot(slotIndex);
        if (itemBySlot == null) {
            return PlayState.STOP;
        }

        ResourceLocation id = event.getAnimatable()
            .getAnimation();
        ConditionArmor conditionArmor = ConditionManager.getArmor(id);
        if (conditionArmor != null) {
            String name = conditionArmor.doTest(player, slotIndex);
            if (StringUtils.isNoneBlank(name)) {
                return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
            }
        }

        ResourceLocation animation = event.getAnimatable()
            .getAnimation();
        String slotName = ConditionArmor.getSlotNameFromIndex(slotIndex);
        String defaultName = slotName + ":default";
        if (GeckoLibCache.getInstance()
            .getAnimations()
            .get(animation).animations.containsKey(defaultName)) {
            return playAnimation(event, defaultName, ILoopType.EDefaultLoopTypes.LOOP);
        }
        return PlayState.STOP;
    }

    private boolean checkSwingAndUse(EntityPlayer player, boolean isMainHand) {
        if (player.isSwingInProgress && BackhandCompat.swingingArm(player) == isMainHand) {
            return false;
        }
        return !player.isUsingItem() || BackhandCompat.getUsedItemHand(player) != isMainHand;
    }
}
