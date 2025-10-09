package com.fox.ysmu.entity;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.AnimationState;
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
    private static final int DATA_WATCHER_IS_FLYING = 21;
    private static final int DATA_WATCHER_IS_SLEEPING = 22;
    public boolean prevSwingInProgress;

    public EntityDisguiseGecko(World world) {
        super(world);
        this.ignoreFrustumCheck = true;
        this.setSize(0.0F, 0.0F);
        this.noClip = true;
    }

    @Override
    public boolean isEntityInvulnerable() {
        return true; // 直接告诉游戏，这个实体是无敌的
    }
    @Override
    public boolean canBeCollidedWith() { return false; }
    @Override
    public AxisAlignedBB getCollisionBox(Entity other) { return null; }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(DATA_WATCHER_MODEL_NAME, "");
        // 为新状态注册DataWatcher。我们用Byte来存储boolean (0=false, 1=true)
        this.dataWatcher.addObject(DATA_WATCHER_IS_FLYING, (byte) 0);
        this.dataWatcher.addObject(DATA_WATCHER_IS_SLEEPING, (byte) 0);
    }

    public void setModel(String modelName) {
        this.dataWatcher.updateObject(DATA_WATCHER_MODEL_NAME, modelName);
    }

    public String getModelName() {
        return this.dataWatcher.getWatchableObjectString(DATA_WATCHER_MODEL_NAME);
    }

    // --- 【新增】飞行状态 Get/Set ---
    public void setFlying(boolean isFlying) { this.dataWatcher.updateObject(DATA_WATCHER_IS_FLYING, (byte) (isFlying ? 1 : 0)); }
    public boolean isFlying() { return this.dataWatcher.getWatchableObjectByte(DATA_WATCHER_IS_FLYING) == 1; }

    // --- 【新增】睡觉状态 Get/Set ---
    public void setSleeping(boolean isSleeping) { this.dataWatcher.updateObject(DATA_WATCHER_IS_SLEEPING, (byte) (isSleeping ? 1 : 0)); }
    public boolean isSleeping() { return this.dataWatcher.getWatchableObjectByte(DATA_WATCHER_IS_SLEEPING) == 1; }

    @Override
    public void registerControllers(AnimationData data) {
        // 一个通用的控制器，用于处理站立和行走动画
        data.addAnimationController(new AnimationController<>(this, "controller", 2, (event) -> {
            // 优先级 0: 睡觉 (最高)
            if (this.isSleeping()) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("sleep", true));
                return PlayState.CONTINUE;
            }
            // 优先级 1: 乘坐/骑乘
            if (this.isRiding()) {
                Entity ridingEntity = this.ridingEntity;
                if (ridingEntity instanceof net.minecraft.entity.item.EntityMinecart || ridingEntity instanceof net.minecraft.entity.item.EntityBoat) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("boat", true));
                } else if (ridingEntity instanceof net.minecraft.entity.passive.EntityHorse) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("ride", true));
                } else if (ridingEntity instanceof net.minecraft.entity.passive.EntityPig) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("ride_pig", true));
                } else {
                    // 默认的乘坐姿势
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("sit", true));
                }
                return PlayState.CONTINUE;
            }
            // 优先级 2: 飞行
            if (this.isFlying()) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("fly", true));
                return PlayState.CONTINUE;
            }
            // 优先级 3: 爬梯子
            if (this.isOnLadder()) {
                if (this.isSneaking()) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("ladder_stillness", true));
                } else if (this.motionY > 0.01) { // 稍微给个阈值防止抖动
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("ladder_up", true));
                } else if (this.motionY < -0.01) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("ladder_down", true));
                } else {
                    // 既没动也没潜行，就静止
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("ladder_stillness", true));
                }
                return PlayState.CONTINUE;
            }
            // 优先级 4: 游泳 (完全浸没)
            // 检查实体是否在水中，并且头部方块也是水
            Block headBlock = this.worldObj.getBlock((int)Math.floor(this.posX), (int)Math.floor(this.posY + this.getEyeHeight()), (int)Math.floor(this.posZ));
            if (this.isInWater() && headBlock instanceof net.minecraft.block.BlockLiquid) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("swim_stand", true));
                return PlayState.CONTINUE;
            }
            // 优先级 5: 跳跃/下落
            if (!this.onGround) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("jump", false));
                return PlayState.CONTINUE;
            }
            // 优先级 6: 潜行
            if (this.isSneaking()) {
                if (event.isMoving()) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("sneak", true));
                } else {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("sneaking", false));
                }
                return PlayState.CONTINUE;
            }
            // 优先级 7: 疾跑
            if (this.isSprinting()) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("run", true));
                return PlayState.CONTINUE;
            }
            if (event.isMoving()) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("walk", true));
            } else {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("idle",true));
            }
            return PlayState.CONTINUE;
        }));
        data.addAnimationController(new AnimationController<>(this, "action_controller", 0, (event) -> {
            // 只要玩家正在挥手，我们就让这个控制器保持“继续”状态
            if (this.isSwingInProgress) {
                // 并且，仅在控制器处于“停止”状态时（即动作的开始），我们才设置动画
                if (event.getController().getAnimationState() == AnimationState.Stopped) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("use_mainhand", false));
                }
                return PlayState.CONTINUE;
            }

            // 一旦 isSwingInProgress 变为 false，我们就立即返回 STOP。
            // 这会强制将控制器的状态重置为 AnimationState.Stopped，
            // 为下一次挥手做好准备。
            return PlayState.STOP;
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
        nbt.setBoolean("IsFlying", isFlying());
        nbt.setBoolean("IsSleeping", isSleeping());
    }



    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        setModel(nbt.getString("ModelName"));
        setFlying(nbt.getBoolean("IsFlying"));
        setSleeping(nbt.getBoolean("IsSleeping"));
    }
}