package com.gts.ysmu.client.animation.molang.functions.ysm;

import com.gts.ysmu.client.animation.molang.functions.physics.SecondOrder;
import com.gts.ysmu.client.animation.molang.PhysicsManager;
import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.util.StringPool;
import com.gts.ysmu.client.animation.molang.functions.physics.IPhysics;
import com.gts.ysmu.geckolib3.core.molang.funciton.entity.EntityFunction;
import com.gts.ysmu.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.Entity;

public class SecondOrderFunction extends EntityFunction {
    @Override
    public Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        int name = arguments.getStringId(context, 0);
        if (name == StringPool.EMPTY_ID) {
            return 0;
        }
        float input = arguments.getAsFloat(context, 1);
        int size = arguments.size();
        float frequency = 1.0f;
        float coefficient = 1.0f;
        float response = 1.0f;
        if (size >= 3) {
            frequency = arguments.getAsFloat(context, 2);
        }
        if (size >= 4) {
            coefficient = arguments.getAsFloat(context, 3);
        }
        if (size >= 5) {
            response = arguments.getAsFloat(context, 4);
        }
        PhysicsManager physicsManager = context.entity().geoInstance().getPhysicsManager();
        IPhysics physics = physicsManager.get(name);
        if (physics == null) {
            physicsManager.put(name, new SecondOrder(input, frequency, coefficient, response));
            return input;
        }
        physics.setArgs(input, frequency, coefficient, response);
        return physics.getValue();
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 2;
    }
}
