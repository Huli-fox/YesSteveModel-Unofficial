package com.fox.ysmu.geckolib3.core.manager;

import com.fox.ysmu.geckolib3.core.IAnimatable;

import java.util.HashMap;


public class SingletonAnimationFactory extends AnimationFactory {
    private final HashMap<Integer, AnimationData> animationDataMap = new HashMap<>();

    public SingletonAnimationFactory(IAnimatable animatable) {
        super(animatable);
    }

    @Override

    public AnimationData getOrCreateAnimationData(Integer uniqueID) {
        if (!this.animationDataMap.containsKey(uniqueID)) {
            AnimationData data = new AnimationData();
            this.animatable.registerControllers(data);
            this.animationDataMap.put(uniqueID, data);
        }
        return this.animationDataMap.get(uniqueID);
    }
}
