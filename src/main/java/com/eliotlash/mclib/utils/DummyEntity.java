package com.eliotlash.mclib.utils;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Dummy entity
 * <p>
 * This class is used in model editor as a player substitution for the model
 * methods.
 */
public class DummyEntity extends EntityLivingBase {
    private final ItemStack[] held;

    public DummyEntity(World worldIn) {
        super(worldIn);

        this.held = new ItemStack[]{null, null, null, null, new ItemStack(Items.diamond_sword)};
    }

    @Override
    public ItemStack getHeldItem() {
        return held[4];
    }

    @Override
    public ItemStack getEquipmentInSlot(int slot) {
        return held[slot];
    }

    @Override
    public void setCurrentItemOrArmor(int p_70062_1_, ItemStack p_70062_2_) {
        this.held[p_70062_1_] = p_70062_2_;
    }

    @Override
    public ItemStack[] getLastActiveItems() {
        return held;
    }
}
