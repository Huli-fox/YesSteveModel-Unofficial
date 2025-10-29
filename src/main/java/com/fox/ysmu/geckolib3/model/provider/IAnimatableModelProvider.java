package com.fox.ysmu.geckolib3.model.provider;

import com.fox.ysmu.util.Keep;
import net.minecraft.util.ResourceLocation;

public interface IAnimatableModelProvider<E> {
    @Keep
    ResourceLocation getAnimationFileLocation(E animatable);
}
