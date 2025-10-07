package com.fox.test.entity;

import net.minecraft.entity.player.EntityPlayer;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class PlayerWrapper implements IAnimatable, IAnimationTickable {
    private final EntityPlayer player;
    private final AnimationFactory factory = new AnimationFactory(this);
    
    public PlayerWrapper(EntityPlayer player) {
        this.player = player;
    }
    
    public EntityPlayer getPlayer() {
        return player;
    }
    
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        // 参考提供的代码实现更完整的动画逻辑
        AnimationBuilder builder = new AnimationBuilder();
        
        // 逻辑顺序很重要，优先判断最特殊的状态
        // 1. 判断是否在空中 (不在地面上)
        if (!player.onGround) {
            builder.addAnimation("jump", true);
        }
        // 2. 判断是否在潜行
        else if (player.isSneaking()) {
            builder.addAnimation("sneak", true);
        }
        // 3. 判断是否在移动 (limbSwingAmount > 0.1F 是一个很好的检测标准)
        else if (player.limbSwingAmount > 0.1F) {
            // 3.1 如果在移动，再判断是否在疾跑
            if (player.isSprinting()) {
                builder.addAnimation("run", true);
            } else {
                builder.addAnimation("walk", true);
            }
        }
        // 4. 如果以上都不是，则播放待机动画
        else {
            builder.addAnimation("idle", true);
        }
        
        // 应用动画
        event.getController().setAnimation(builder);
        return PlayState.CONTINUE;
    }
    
    @Override
    public void registerControllers(AnimationData data) {
        // 注册一个默认的控制器，使用10 ticks的过渡时间
        data.addAnimationController(new AnimationController<>(this, "controller", 10, this::predicate));
    }
    
    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
    
    @Override
    public int tickTimer() {
        return player.ticksExisted;
    }
    
    // 当玩家向前移动时，将身体朝向调整为视角方向
    public void updateMovingDirection() {
        // 检查玩家是否在移动且移动幅度大于阈值
        if (player.limbSwingAmount > 0.1F) {
            // 将身体朝向设置为头部朝向，使身体跟随视角
            player.renderYawOffset = player.rotationYawHead;
        }
    }
}