package software.bernie.geckolib3.util;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.core.manager.InstancedAnimationFactory;
import software.bernie.geckolib3.core.manager.SingletonAnimationFactory;

import java.util.Objects;

public class GeckoLibUtil {
    public static AnimationFactory createFactory(IAnimatable animatable, boolean singletonObject) {
        return singletonObject ? new SingletonAnimationFactory(animatable) : new InstancedAnimationFactory(animatable);
    }
}
