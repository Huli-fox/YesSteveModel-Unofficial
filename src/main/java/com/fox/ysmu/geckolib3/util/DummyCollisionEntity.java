package com.fox.ysmu.geckolib3.util;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class DummyCollisionEntity extends Entity {
    public AxisAlignedBB bb;

    public DummyCollisionEntity(World p_i1582_1_) {
        super(p_i1582_1_);
    }

    public DummyCollisionEntity(World p_i1582_1_, AxisAlignedBB bb) {
        super(p_i1582_1_);
        this.bb = bb;
    }

    @Override
    protected void entityInit() {

    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {

    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity p_70114_1_) {
        return bb;
    }
}
