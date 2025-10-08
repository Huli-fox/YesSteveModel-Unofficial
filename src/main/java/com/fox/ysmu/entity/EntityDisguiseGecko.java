package com.fox.ysmu.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.PlayState;
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
        data.addAnimationController(new AnimationController<>(this, "controller", 2, (event) -> {
            // 优先级 1: 跳跃 (Jump)
            // onGround 是实体是否在地面上的标志
            if (!this.onGround) {
                // 假设动画名为 "jump"，并且不循环播放 (一次性动画)
                event.getController().setAnimation(new AnimationBuilder().addAnimation("jump", false));
                return PlayState.CONTINUE;
            }
            // 优先级 2: 潜行 (Sneaking)
            if (this.isSneaking()) {
                if (event.isMoving()) {
                    // 潜行时移动，播放 "sneak" 动画 (循环)
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("sneak", true));
                } else {
                    // 潜行时不动，播放 "sneaking" 动画 (循环)
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("sneaking", false));
                }
                return PlayState.CONTINUE;
            }
            // 优先级 3: 疾跑 (Sprinting)
            // isSprinting() 是实体是否在疾跑的标志
            if (this.isSprinting()) {
                // 疾跑时，播放 "run" 动画 (循环)
                event.getController().setAnimation(new AnimationBuilder().addAnimation("run", true));
                return PlayState.CONTINUE;
            }
            if (event.isMoving()) {
                // 假设所有模型都有一个名为"walk"的动画
                event.getController().setAnimation(new AnimationBuilder().addAnimation("walk", true));
            } else {
                // 假设所有模型都有一个名为"idle"的动画
                event.getController().setAnimation(new AnimationBuilder().addAnimation("idle",true));
            }
            return PlayState.CONTINUE;
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