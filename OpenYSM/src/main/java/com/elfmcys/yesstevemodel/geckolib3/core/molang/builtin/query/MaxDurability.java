package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class MaxDurability extends LivingEntityFunction {
    @Override
    public Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        EquipmentSlot slot = MolangUtils.parseSlotType(context.entity(), arguments.getAsString(context, 0));
        if (slot == null) {
            return 0;
        }
        return context.entity().entity().getItemBySlot(slot).getMaxDamage();
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
