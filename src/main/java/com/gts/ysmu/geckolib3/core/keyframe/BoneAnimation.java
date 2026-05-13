package com.gts.ysmu.geckolib3.core.keyframe;

import com.gts.ysmu.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.gts.ysmu.geckolib3.core.molang.util.StringPool;

import java.util.List;

public class BoneAnimation {

    public final String boneName;

    public final int boneId;

    public final List<BoneKeyFrame> rotationKeyFrames;

    public final List<BoneKeyFrame> positionKeyFrames;

    public final List<BoneKeyFrame> scaleKeyFrames;

    public BoneAnimation(String boneName, List<BoneKeyFrame> rotationKeyFrames, List<BoneKeyFrame> positionKeyFrames, List<BoneKeyFrame> scaleKeyFrames) {
        this.boneName = boneName;
        this.boneId = StringPool.computeIfAbsent(boneName);
        this.rotationKeyFrames = rotationKeyFrames;
        this.positionKeyFrames = positionKeyFrames;
        this.scaleKeyFrames = scaleKeyFrames;
    }
}
