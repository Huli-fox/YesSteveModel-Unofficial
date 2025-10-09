package com.fox.ysmu.entity;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
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

import java.util.UUID;

public class EntityDisguiseGecko extends EntityLivingBase implements IAnimatable, IAnimationTickable {
    private final ItemStack[] equipment = new ItemStack[5];
    private final AnimationFactory factory = new AnimationFactory(this);
    private static final int DATA_WATCHER_MODEL_NAME = 20;
    private static final int DATA_WATCHER_IS_FLYING = 21;
    private static final int DATA_WATCHER_IS_SLEEPING = 22;
    private static final int DATA_WATCHER_RIDING_STATE = 23;

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
    public void mountEntity(Entity entity) {
        // 不执行任何操作，阻止伪装实体骑乘任何实体
    }
    @Override
    public boolean canBeCollidedWith() {
        return false; // 关闭碰撞检测，避免被交通工具视为障碍
    }
    @Override
    public AxisAlignedBB getCollisionBox(Entity other) {
        return null; // 无碰撞箱
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(DATA_WATCHER_MODEL_NAME, "");
        // 为新状态注册DataWatcher。我们用Byte来存储boolean (0=false, 1=true)
        this.dataWatcher.addObject(DATA_WATCHER_IS_FLYING, (byte) 0);
        this.dataWatcher.addObject(DATA_WATCHER_IS_SLEEPING, (byte) 0);
        this.dataWatcher.addObject(DATA_WATCHER_RIDING_STATE, 0);
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
    
    // --- 【新增】骑乘状态 Get/Set ---
    public void setRidingState(int state) { this.dataWatcher.updateObject(DATA_WATCHER_RIDING_STATE, state); }
    public int getRidingState() { return this.dataWatcher.getWatchableObjectInt(DATA_WATCHER_RIDING_STATE); }

    /**
     * 通过UUID获取玩家实体
     * @param uuid 玩家的UUID
     * @return 对应的EntityPlayer实体，如果找不到则返回null
     */
    private EntityPlayer getPlayerByUUID(UUID uuid) {
        if (uuid == null) return null;
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            // 遍历所有玩家
            for (Object playerObj : server.getConfigurationManager().playerEntityList) {
                EntityPlayer player = (EntityPlayer) playerObj;
                if (player.getUniqueID().equals(uuid)) {
                    return player;
                }
            }
        }
        return null;
    }

    @Override
    public void registerControllers(AnimationData data) {
        // 一个通用的控制器，用于处理站立和行走动画
        data.addAnimationController(new AnimationController<>(this, "controller", 2, (event) -> {
            // 优先级 0: 睡觉 (最高)
            if (this.isSleeping()) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("sleep", true));
                return PlayState.CONTINUE;
            }
            // 通过玩家同步的状态判断是否骑乘（而非自身ridingEntity）
            // 注意：需要在syncEntityStateFromServer中同步玩家的isRiding状态到伪装实体
            EntityPlayer player = getPlayerByUUID(this.getUniqueID());
            boolean isPlayerRiding = player != null && player.isRiding();
            // 优先级 1: 乘坐/骑乘（基于玩家状态）
            int rideState = this.getRidingState();
            if (rideState > 0) { // 只要在骑乘...
                switch (rideState) {
                    case 1: // 坐
                        event.getController().setAnimation(new AnimationBuilder().addAnimation("sit", true));
                        break;
                    case 2: // 骑马
                        event.getController().setAnimation(new AnimationBuilder().addAnimation("ride", true));
                        break;
                    case 3: // 骑猪
                        event.getController().setAnimation(new AnimationBuilder().addAnimation("ride_pig", true));
                        break;
                    default: // 默认也用坐姿
                        event.getController().setAnimation(new AnimationBuilder().addAnimation("sit", true));
                        break;
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
        AnimationController<?> controller = event.getController();

            // 触发条件: 玩家正在挥手 并且 控制器当前是空闲的
            if (this.isSwingInProgress && controller.getAnimationState() == AnimationState.Stopped) {
                // 强制标记控制器需要重新加载动画
                // 防止与上次相同动画名时被判定为“重复动画”而不触发
                controller.markNeedsReload();
                // 设置一次性动画
                controller.setAnimation(new AnimationBuilder().addAnimation("use_mainhand", false));
                // 开始播放动画，并让控制器继续处理
                return PlayState.CONTINUE;
        }
        // 若动画正在播放中，则保持继续播放
        if (controller.getAnimationState() == AnimationState.Running) {
            return PlayState.CONTINUE;
        }

        // 默认条件: 如果既没有触发新动画，也没有动画在播放，就彻底停止并重置控制器
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
        nbt.setInteger("RidingState", getRidingState());
    }



    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        setModel(nbt.getString("ModelName"));
        setFlying(nbt.getBoolean("IsFlying"));
        setSleeping(nbt.getBoolean("IsSleeping"));
        setRidingState(nbt.getInteger("RidingState"));
    }

    @Override
    public ItemStack getHeldItem() {
        return this.equipment[0];
    }

    @Override
    public ItemStack getEquipmentInSlot(int slot) {
        return this.equipment[slot];
    }

    @Override
    public void setCurrentItemOrArmor(int slot, ItemStack stack) {
        this.equipment[slot] = stack;
    }

    @Override
    public ItemStack[] getLastActiveItems() {
        return this.equipment;
    }
}
