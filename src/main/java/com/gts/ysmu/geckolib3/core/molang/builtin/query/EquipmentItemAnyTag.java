package com.gts.ysmu.geckolib3.core.molang.builtin.query;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.util.MolangUtils;
import com.gts.ysmu.geckolib3.core.molang.funciton.entity.LivingEntityFunction;
import com.gts.ysmu.molang.runtime.ExecutionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class EquipmentItemAnyTag extends LivingEntityFunction {
    @Override
    public Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        EquipmentSlot slotType = MolangUtils.parseSlotType(context.entity(), arguments.getAsString(context, 0));
        if (slotType == null) {
            return null;
        }
        ItemStack stack = context.entity().entity().getItemBySlot(slotType);
        if (stack.isEmpty()) {
            return false;
        }
        for (int i = 1; i < arguments.size(); i++) {
            ResourceLocation key = arguments.getResourceLocation(context, i);
            if (key == null) {
                return null;
            }
            if (stack.is(ForgeRegistries.ITEMS.tags().createTagKey(key))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 2;
    }
}
