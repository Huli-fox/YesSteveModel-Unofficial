package com.gts.ysmu.client.animation.predicate;

import com.gts.ysmu.client.animation.IAnimationPredicate;
import com.gts.ysmu.client.entity.LivingAnimatable;
import com.gts.ysmu.client.animation.condition.ConditionHold;
import com.gts.ysmu.geckolib3.core.builder.ILoopType;
import com.gts.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.gts.ysmu.geckolib3.core.enums.PlayState;
import com.gts.ysmu.client.entity.IPreviewAnimatable;
import com.gts.ysmu.client.entity.LivingEntityFrameState;
import com.gts.ysmu.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.StringUtils;

public class MainHandHoldPredicate implements IAnimationPredicate<LivingAnimatable<?>> {
    @Override
    public PlayState predicate(AnimationEvent<LivingAnimatable<?>> event, ExpressionEvaluator<?> evaluator) {
        LivingEntity entity = event.getAnimatable().getEntity();
        if (entity == null || (event.getAnimatable() instanceof IPreviewAnimatable)) {
            return PlayState.STOP;
        }
        if (!checkSwingAndUse(entity, InteractionHand.MAIN_HAND)) {
            return PlayState.PAUSE;
        }
        int i = event.getAnimatable().getModelAssembly().getModelData().getFormatVersion();
        ItemStack mainHandItem = entity.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHandItem.is(Items.CROSSBOW) && CrossbowItem.isCharged(mainHandItem)) {
            return IAnimationPredicate.playAnimationWithValid(event, "hold_mainhand:charged_crossbow", ILoopType.EDefaultLoopTypes.LOOP, i);
        }
        boolean isFishing = (entity instanceof Player) && ((Player) entity).fishing != null;
        if (isFishing) {
            return IAnimationPredicate.playAnimationWithValid(event, "hold_mainhand:fishing", ILoopType.EDefaultLoopTypes.LOOP, i);
        }
        LivingEntityFrameState<?> c0675x43c72e02Mo1215x3cfc56ba = ((LivingAnimatable) event.getAnimatable()).getPositionTracker();
        if (!isSameItem(mainHandItem, c0675x43c72e02Mo1215x3cfc56ba, InteractionHand.MAIN_HAND)) {
            c0675x43c72e02Mo1215x3cfc56ba.setHandItemsForAnimation(mainHandItem, InteractionHand.MAIN_HAND);
            event.getController().stopTransition();
        }
        ConditionHold conditionHold = event.getAnimatable().getModelConfig().getHoldMainhand();
        if (conditionHold != null) {
            String str = conditionHold.doTest(entity, InteractionHand.MAIN_HAND);
            if (StringUtils.isNoneBlank(str)) {
                return IAnimationPredicate.playAnimationWithValid(event, str, ILoopType.EDefaultLoopTypes.LOOP, i);
            }
        }
        return PlayState.STOP;
    }

    private boolean isSameItem(ItemStack itemStack, LivingEntityFrameState<?> frameState, InteractionHand hand) {
        ItemStack preItem = frameState.getHandItemsForAnimation(hand);
        if (preItem.isDamaged()) {
            return ItemStack.isSameItem(itemStack, preItem);
        }
        return ItemStack.matches(itemStack, preItem);
    }

    private boolean checkSwingAndUse(LivingEntity entity, InteractionHand hand) {
        if (entity.swinging && entity.swingingArm == hand) {
            return false;
        }
        return !entity.isUsingItem() || entity.getUsedItemHand() != hand;
    }
}
