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

public class IsItemNameAny extends LivingEntityFunction {
    @Override
    public Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        ResourceLocation key;
        EquipmentSlot slotType = MolangUtils.parseSlotType(context.entity(), arguments.getAsString(context, 0));
        if (slotType == null) {
            return null;
        }
        ItemStack stack = context.entity().entity().getItemBySlot(slotType);
        if (!stack.isEmpty() && (key = ForgeRegistries.ITEMS.getKey(stack.getItem())) != null) {
            for (int i = 1; i < arguments.size(); i++) {
                ResourceLocation location = arguments.getResourceLocation(context, i);
                if (location == null) {
                    return null;
                }
                if (location.equals(key)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 2;
    }
}
