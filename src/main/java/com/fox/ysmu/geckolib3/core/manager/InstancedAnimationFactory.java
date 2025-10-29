package com.fox.ysmu.geckolib3.core.manager;

import com.fox.ysmu.geckolib3.core.IAnimatable;
import com.fox.ysmu.util.Keep;

public class InstancedAnimationFactory extends AnimationFactory {
    private AnimationData animationData;

    public InstancedAnimationFactory(IAnimatable animatable) {
        super(animatable);
    }

    @Override
    @Keep
    public AnimationData getOrCreateAnimationData(int uniqueID) {
        if (this.animationData == null) {
            this.animationData = new AnimationData();
            this.animatable.registerControllers(this.animationData);
        }
        return this.animationData;
    }
}
