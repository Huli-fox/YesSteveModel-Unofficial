package com.fox.test.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class CustomEntity extends EntityCreature implements IAnimatable, IAnimationTickable {
    private final AnimationFactory factory = new AnimationFactory(this);

    public CustomEntity(World worldIn) {
        super(worldIn);
        // 让实体即使在视野外也渲染
        this.ignoreFrustumCheck = true;
        // 添加一个简单的AI，让实体朝向最近的玩家
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        // 设置实体的碰撞箱大小
        this.setSize(1.0F, 1.0F);
    }

    // 动画播放的逻辑判断
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        // 让控制器循环播放名为 "animation.main.idle" 的动画
        // ***重要***: 请将 "animation.main.idle" 替换为您在BlockBench中为动画设置的真实名称
        event.getController().setAnimation(new AnimationBuilder().addAnimation("run"));
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        // 注册一个动画控制器
        // "controller" 是控制器的名字
        // 5 是动画切换时的平滑过渡时间（单位：tick），可以设为0以立即切换
        data.addAnimationController(new AnimationController<>(this, "controller", 5, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Override
    public int tickTimer() {
        return ticksExisted;
    }
}