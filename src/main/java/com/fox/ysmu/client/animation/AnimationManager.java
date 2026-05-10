package com.fox.ysmu.client.animation;

import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.fox.ysmu.client.animation.condition.*;
import com.fox.ysmu.client.animation.controller.OpenYsmPlayerControllerRuntime;
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
    private final Map<UUID, Integer> swingProgressByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> useDurationByPlayer = new ConcurrentHashMap<>();

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
        PlayState controllerState = OpenYsmPlayerControllerRuntime.tryApply(event);
        if (controllerState != null) {
            return controllerState;
        }
        return playLoopAnimation(event, animationName);
    }

    public PlayState predicateOpenYsmSlot(AnimationEvent<CustomPlayerEntity> event) {
        if (Minecraft.getMinecraft()
            .isGamePaused()) {
            return PlayState.STOP;
        }
        PlayState controllerState = OpenYsmPlayerControllerRuntime.tryApply(event);
        return controllerState == null ? PlayState.STOP : controllerState;
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
        PlayState controllerState = OpenYsmPlayerControllerRuntime.tryApply(event);
        if (controllerState != null) {
            return controllerState;
        }
        for (int i = Priority.HIGHEST; i <= Priority.LOWEST; i++) {
            if (!data.containsKey(i)) {
                continue;
            }
            LinkedList<AnimationState> states = data.get(i);
            for (AnimationState state : states) {
                if (state.getPredicate().test(player, event)) {
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
        PlayState controllerState = OpenYsmPlayerControllerRuntime.tryApply(event);
        if (controllerState != null) {
            return controllerState;
        }

        // 修改为使用BackhandCompat兼容层
        ItemStack offhandItem = BackhandCompat.getOffhandItem(player);
        if (offhandItem != null && checkSwingAndUse(player, false)) {
            return playIfPresent(event, findHoldAnimation(event, player, false));
        }
        return PlayState.STOP;
    }

    public PlayState predicateMainhandHold(AnimationEvent<CustomPlayerEntity> event) {
        EntityPlayer player = event.getAnimatable()
            .getPlayer();
        if (player == null) {
            return PlayState.STOP;
        }
        PlayState controllerState = OpenYsmPlayerControllerRuntime.tryApply(event);
        if (controllerState != null) {
            return controllerState;
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
            return playIfPresent(event, findHoldAnimation(event, player, true));
        }
        return PlayState.STOP;
    }

    public PlayState predicateSwing(AnimationEvent<CustomPlayerEntity> event) {
        EntityPlayer player = event.getAnimatable()
            .getPlayer();
        if (player == null) {
            return PlayState.STOP;
        }
        PlayState controllerState = OpenYsmPlayerControllerRuntime.tryApply(event);
        if (controllerState != null) {
            return controllerState;
        }
        if (!player.isSwingInProgress) {
            swingProgressByPlayer.remove(player.getUniqueID());
            return PlayState.STOP;
        }
        if (!player.isPlayerSleeping()) {
            if (markSwingStart(player)) {
                event.getController().shouldResetTick = true;
                event.getController().markNeedsReload();
                event.getController()
                    .adjustTick(0);
            }
            String conditionalAnimation = findSwingAnimation(event, player);
            if (StringUtils.isNoneBlank(conditionalAnimation)) {
                return playAnimation(event, conditionalAnimation, ILoopType.EDefaultLoopTypes.PLAY_ONCE);
            }
            return playAnimation(event, "swing_hand", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
        }
        return PlayState.STOP;
    }

    private boolean markSwingStart(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        if (!player.isSwingInProgress) {
            swingProgressByPlayer.remove(playerId);
            return false;
        }
        int currentProgress = player.swingProgressInt;
        Integer previousProgress = swingProgressByPlayer.put(playerId, currentProgress);
        return previousProgress == null || currentProgress < previousProgress;
    }

    public PlayState predicateUse(AnimationEvent<CustomPlayerEntity> event) {
        EntityPlayer player = event.getAnimatable()
            .getPlayer();
        if (player == null) {
            return PlayState.STOP;
        }
        PlayState controllerState = OpenYsmPlayerControllerRuntime.tryApply(event);
        if (controllerState != null) {
            return controllerState;
        }
        if (player.isUsingItem() && !player.isPlayerSleeping()) {
            if (markUseStart(player)) {
                event.getController().shouldResetTick = true;
                event.getController().markNeedsReload();
                event.getController()
                    .adjustTick(0);
            }
            boolean isMainHand = BackhandCompat.getUsedItemHand(player);
            String conditionalAnimation = findUseAnimation(event, player, isMainHand);
            if (StringUtils.isNoneBlank(conditionalAnimation)) {
                return playAnimation(event, conditionalAnimation);
            }
            return playAnimation(event, isMainHand ? "use_mainhand" : "use_offhand", ILoopType.EDefaultLoopTypes.LOOP);
        }
        useDurationByPlayer.remove(player.getUniqueID());
        return PlayState.STOP;
    }

    private boolean markUseStart(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        if (!player.isUsingItem()) {
            useDurationByPlayer.remove(playerId);
            return false;
        }
        int currentDuration = player.getItemInUseDuration();
        Integer previousDuration = useDurationByPlayer.put(playerId, currentDuration);
        return previousDuration == null || currentDuration < previousDuration;
    }

    public PlayState predicateArmor(AnimationEvent<CustomPlayerEntity> event, int slotIndex) {
        EntityPlayer player = event.getAnimatable()
            .getPlayer();
        if (player == null) {
            return PlayState.STOP;
        }
        PlayState controllerState = OpenYsmPlayerControllerRuntime.tryApply(event);
        if (controllerState != null) {
            return controllerState;
        }
        ItemStack itemBySlot = player.getEquipmentInSlot(slotIndex);
        if (itemBySlot == null) {
            return PlayState.STOP;
        }

        String conditionalAnimation = findArmorAnimation(event, player, slotIndex);
        if (StringUtils.isNoneBlank(conditionalAnimation)) {
            return playLoopAnimation(event, conditionalAnimation);
        }

        ResourceLocation animation = getAnimationId(event);
        String slotName = ConditionArmor.getSlotNameFromIndex(slotIndex);
        String defaultName = slotName + ":default";
        if (GeckoLibCache.getInstance()
            .getAnimations()
            .get(animation).animations.containsKey(defaultName)) {
            return playAnimation(event, defaultName, ILoopType.EDefaultLoopTypes.LOOP);
        }
        return PlayState.STOP;
    }

    private static ResourceLocation getAnimationId(AnimationEvent<CustomPlayerEntity> event) {
        return event.getAnimatable()
            .getAnimation();
    }

    private static PlayState playIfPresent(AnimationEvent<CustomPlayerEntity> event, String animationName) {
        if (StringUtils.isNoneBlank(animationName)) {
            return playAnimation(event, animationName);
        }
        return PlayState.STOP;
    }

    private static String findHoldAnimation(AnimationEvent<CustomPlayerEntity> event, EntityPlayer player,
        boolean isMainHand) {
        ResourceLocation id = getAnimationId(event);
        ConditionalHold conditionalHold = isMainHand ? ConditionManager.getHoldMainhand(id)
            : ConditionManager.getHoldOffhand(id);
        return conditionalHold == null ? null : conditionalHold.doTest(player, isMainHand);
    }

    private static String findSwingAnimation(AnimationEvent<CustomPlayerEntity> event, EntityPlayer player) {
        ConditionalSwing conditionalSwing = ConditionManager.getSwing(getAnimationId(event));
        return conditionalSwing == null ? null : conditionalSwing.doTest(player, BackhandCompat.swingingArm(player));
    }

    private static String findUseAnimation(AnimationEvent<CustomPlayerEntity> event, EntityPlayer player,
        boolean isMainHand) {
        ResourceLocation id = getAnimationId(event);
        ConditionalUse conditionalUse = isMainHand ? ConditionManager.getUseMainhand(id)
            : ConditionManager.getUseOffhand(id);
        return conditionalUse == null ? null : conditionalUse.doTest(player, isMainHand);
    }

    private static String findArmorAnimation(AnimationEvent<CustomPlayerEntity> event, EntityPlayer player,
        int slotIndex) {
        ConditionArmor conditionArmor = ConditionManager.getArmor(getAnimationId(event));
        return conditionArmor == null ? null : conditionArmor.doTest(player, slotIndex);
    }

    private boolean checkSwingAndUse(EntityPlayer player, boolean isMainHand) {
        if (player.isSwingInProgress && BackhandCompat.swingingArm(player) == isMainHand) {
            return false;
        }
        return !player.isUsingItem() || BackhandCompat.getUsedItemHand(player) != isMainHand;
    }
}
