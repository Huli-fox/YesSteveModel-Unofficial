/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.fox.ysmu.geckolib3.core.keyframe;

import com.fox.ysmu.geckolib3.core.processor.IBone;

public record BoneAnimationQueue(IBone bone, AnimationPointQueue rotationXQueue, AnimationPointQueue rotationYQueue,
                                 AnimationPointQueue rotationZQueue, AnimationPointQueue positionXQueue,
                                 AnimationPointQueue positionYQueue,
                                 AnimationPointQueue positionZQueue, AnimationPointQueue scaleXQueue,
                                 AnimationPointQueue scaleYQueue,
                                 AnimationPointQueue scaleZQueue) {

    public BoneAnimationQueue(IBone bone) {
        this(bone, new AnimationPointQueue(), new AnimationPointQueue(), new AnimationPointQueue(),
                new AnimationPointQueue(), new AnimationPointQueue(), new AnimationPointQueue(),
                new AnimationPointQueue(), new AnimationPointQueue(), new AnimationPointQueue());
    }
}
