package com.gts.ysmu.client.animation.molang.functions.ysm;

import com.gts.ysmu.capability.PlayerCapability;
import com.gts.ysmu.client.entity.CustomPlayerEntity;
import com.gts.ysmu.geckolib3.core.AnimatableEntity;
import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.entity.AbstractClientPlayerFunction;
import com.gts.ysmu.molang.runtime.ExecutionContext;
import com.gts.ysmu.network.NetworkHandler;
import com.gts.ysmu.network.message.C2SSyncAnimationExpressionPacket;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;

public class Sync extends AbstractClientPlayerFunction {
    private static final int MAX_ARGS = 16;

    @Override
    public Object eval(ExecutionContext<IContext<AbstractClientPlayer>> context, ArgumentCollection arguments) {
        if (!context.entity().isClientSide()) {
            return null;
        }
        if ((context.entity().geoInstance() instanceof PlayerCapability) && NetworkHandler.isClientConnected()) {
            if (context.entity().entity() instanceof LocalPlayer) {
                NetworkHandler.sendToServer(new C2SSyncAnimationExpressionPacket(collectArgs(context, arguments)));
                return null;
            }
            return null;
        }
        AnimatableEntity<?> animatableEntity = context.entity().geoInstance();
        if (animatableEntity instanceof CustomPlayerEntity) {
            ((CustomPlayerEntity) animatableEntity).executeAnimationExpression(collectArgs(context, arguments));
            return null;
        }
        return null;
    }

    private static FloatArrayList collectArgs(ExecutionContext<IContext<AbstractClientPlayer>> context, ArgumentCollection arguments) {
        FloatArrayList floatArrayList = new FloatArrayList(arguments.size());
        for (int i = 0; i < arguments.size(); i++) {
            floatArrayList.add(arguments.getAsFloat(context, i));
        }
        return floatArrayList;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size <= MAX_ARGS;
    }
}
