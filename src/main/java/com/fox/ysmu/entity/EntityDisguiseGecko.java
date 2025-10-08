package com.fox.ysmu.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class EntityDisguiseGecko extends EntityCreature implements IAnimatable, IAnimationTickable {

    private final AnimationFactory factory = new AnimationFactory(this);
    private static final int DATA_WATCHER_MODEL_NAME = 20;

    public EntityDisguiseGecko(World world) {
        super(world);
        this.ignoreFrustumCheck = true;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(DATA_WATCHER_MODEL_NAME, "");
    }

    public void setModel(String modelName) {
        this.dataWatcher.updateObject(DATA_WATCHER_MODEL_NAME, modelName);
    }

    public String getModelName() {
        return this.dataWatcher.getWatchableObjectString(DATA_WATCHER_MODEL_NAME);
    }

    // --- 完全遵循教程的动画逻辑 ---

    @Override
    public void registerControllers(AnimationData data) {
        // 一个通用的控制器，用于处理站立和行走动画
        data.addAnimationController(new AnimationController<>(this, "controller", 20, (event) -> {
            if (event.isMoving()) {
                // 假设所有模型都有一个名为"walk"的动画
                event.getController().setAnimation(new AnimationBuilder().addAnimation("walk", true));
            } else {
                // 假设所有模型都有一个名为"idle"的动画
                event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", true));
            }
            return software.bernie.geckolib3.core.PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Override
    public int tickTimer() {
        return this.ticksExisted;
    }

    // --- NBT 存储 ---
    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setString("ModelName", getModelName());
    }



    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        setModel(nbt.getString("ModelName"));
    }
}