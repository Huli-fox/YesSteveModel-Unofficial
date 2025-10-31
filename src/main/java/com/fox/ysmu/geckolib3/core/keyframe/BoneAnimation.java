//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.fox.ysmu.geckolib3.core.keyframe;

import com.fox.ysmu.mclib.math.IValue;

import java.io.Serializable;

public class BoneAnimation implements Serializable {
    private static final long serialVersionUID = 42L;
    public String boneName;
    public VectorKeyFrameList<KeyFrame<IValue>> rotationKeyFrames;
    public VectorKeyFrameList<KeyFrame<IValue>> positionKeyFrames;
    public VectorKeyFrameList<KeyFrame<IValue>> scaleKeyFrames;

    public BoneAnimation() {
    }
}
