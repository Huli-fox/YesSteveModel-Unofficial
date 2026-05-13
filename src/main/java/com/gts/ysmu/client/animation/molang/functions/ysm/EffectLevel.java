package com.gts.ysmu.client.animation.molang.functions.ysm;

import com.gts.ysmu.capability.PlayerCapability;
import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.funciton.ContextFunction;
import com.gts.ysmu.mixin.client.ArrowEntityAccessor;
import com.gts.ysmu.molang.runtime.ExecutionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraftforge.registries.ForgeRegistries;

public class EffectLevel extends ContextFunction<Entity> {
    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 1;
    }

    @Override
    public Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        int effects = 0;

        for (int i = 0; i < arguments.size(); i++) {
            ResourceLocation effectId = arguments.getResourceLocation(context, i);
            if (effectId != null) {
                MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
                if (mobEffect != null) {
                    if (context.entity().geoInstance() instanceof PlayerCapability cap
                            && !cap.isLocalPlayerModel()) {
                        effects += cap.getPositionTracker().getEffectAmplifier(mobEffect);
                    } else if (((IContext<?>)context.entity()).entity() instanceof LivingEntity) {
                        MobEffectInstance mobEffectInstance = ((LivingEntity)((IContext<?>)context.entity()).entity())
                                .getEffect(mobEffect);
                        if (mobEffectInstance != null) {
                            effects += mobEffectInstance.getAmplifier() + 1;
                        }
                    } else {
                        if (!(((IContext<?>)context.entity()).entity() instanceof Arrow)) {
                            return null;
                        }

                        for (MobEffectInstance mobEffectInstance : ((ArrowEntityAccessor)((IContext<?>)context.entity()).entity())
                                .getEffects()) {
                            if (mobEffectInstance.getEffect() == mobEffect) {
                                effects += mobEffectInstance.getAmplifier() + 1;
                                break;
                            }
                        }
                    }
                }
            }
        }

        return effects;
    }
}
