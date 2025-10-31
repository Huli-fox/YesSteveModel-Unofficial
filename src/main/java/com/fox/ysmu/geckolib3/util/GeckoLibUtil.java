package com.fox.ysmu.geckolib3.util;

import net.minecraft.item.ItemStack;
import com.fox.ysmu.geckolib3.core.controller.AnimationController;
import com.fox.ysmu.geckolib3.core.manager.AnimationFactory;

import java.util.Objects;

public class GeckoLibUtil {
    public static int getIDFromStack(ItemStack stack) {
        return Objects.hash(stack.getItem(), stack.stackSize,
            stack.hasTagCompound() ? stack.getTagCompound().toString() : 1);
    }

    @SuppressWarnings("rawtypes")
    public static AnimationController getControllerForStack(AnimationFactory factory, ItemStack stack,
                                                            String controllerName) {
        return factory.getOrCreateAnimationData(getIDFromStack(stack)).getAnimationControllers().get(controllerName);
    }
}
