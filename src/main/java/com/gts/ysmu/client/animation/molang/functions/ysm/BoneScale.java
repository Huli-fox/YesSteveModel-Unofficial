package com.gts.ysmu.client.animation.molang.functions.ysm;

import com.gts.ysmu.geckolib3.core.processor.IBone;
import com.gts.ysmu.client.animation.molang.struct.Vec3fStruct;
import org.jetbrains.annotations.NotNull;

public final class BoneScale extends BoneParamFunction {
    @Override
    public Vec3fStruct getParam(@NotNull IBone bone) {
        return new BoneScaleStruct(bone);
    }

    private static final class BoneScaleStruct extends Vec3fStruct {

        private final IBone boneTransform;

        public BoneScaleStruct(IBone bone) {
            this.boneTransform = bone;
        }

        @Override
        public float getX() {
            return this.boneTransform.getScaleX();
        }

        @Override
        public float getY() {
            return this.boneTransform.getScaleY();
        }

        @Override
        public float getZ() {
            return this.boneTransform.getScaleZ();
        }
    }
}
