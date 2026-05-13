package com.gts.ysmu.client.animation.molang.functions.ysm;

import com.gts.ysmu.geckolib3.core.processor.IBone;
import com.gts.ysmu.client.animation.molang.struct.Vec3fStruct;
import org.jetbrains.annotations.NotNull;

public final class BonePosition extends BoneParamFunction {
    @Override
    public Vec3fStruct getParam(@NotNull IBone bone) {
        return new BonePositionStruct(bone);
    }

    private static final class BonePositionStruct extends Vec3fStruct {

        private final IBone boneTransform;

        public BonePositionStruct(IBone bone) {
            this.boneTransform = bone;
        }

        @Override
        public float getX() {
            return this.boneTransform.getPositionX();
        }

        @Override
        public float getY() {
            return this.boneTransform.getPositionY();
        }

        @Override
        public float getZ() {
            return this.boneTransform.getPositionZ();
        }
    }
}
