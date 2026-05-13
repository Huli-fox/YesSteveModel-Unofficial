package com.gts.ysmu.client.animation.molang.functions.ysm;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.entity.EntityFunction;
import com.gts.ysmu.geckolib3.core.molang.util.StringPool;
import com.gts.ysmu.geckolib3.core.processor.IBone;
import com.gts.ysmu.client.animation.molang.struct.Vec3fStruct;
import com.gts.ysmu.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public abstract class BoneParamFunction extends EntityFunction {
    public abstract Vec3fStruct getParam(@NotNull IBone bone);

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }

    @Override
    public Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        IBone bone;
        int name = arguments.getStringId(context, 0);
        if (name == StringPool.EMPTY_ID || (bone = context.entity().geoInstance().getBone(name)) == null) {
            return null;
        }
        return getParam(bone);
    }
}
