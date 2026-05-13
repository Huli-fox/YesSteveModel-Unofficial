package com.gts.ysmu.client.entity;

import com.gts.ysmu.geckolib3.core.EntityFrameStateTracker;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class LivingEntityFrameState<T extends LivingEntity> extends EntityFrameStateTracker<T> {

    private ItemStack mainHandItem;

    private ItemStack offHandItem;

    public LivingEntityFrameState(T t) {
        super(t);
        this.mainHandItem = ItemStack.EMPTY;
        this.offHandItem = ItemStack.EMPTY;
    }

    @Override
    public void reset() {
        this.mainHandItem = ItemStack.EMPTY;
        this.offHandItem = ItemStack.EMPTY;
        super.reset();
    }

    public ItemStack getHandItemsForAnimation(InteractionHand interactionHand) {
        if (interactionHand == InteractionHand.MAIN_HAND) {
            return this.mainHandItem;
        }
        return this.offHandItem;
    }

    public void setHandItemsForAnimation(ItemStack itemStack, InteractionHand interactionHand) {
        if (interactionHand == InteractionHand.MAIN_HAND) {
            this.mainHandItem = itemStack;
        } else {
            this.offHandItem = itemStack;
        }
    }
}
