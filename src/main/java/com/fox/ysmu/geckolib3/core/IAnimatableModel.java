package com.fox.ysmu.geckolib3.core;

import com.fox.ysmu.geckolib3.core.builder.Animation;
import com.fox.ysmu.geckolib3.core.event.predicate.AnimationEvent;
import com.fox.ysmu.geckolib3.core.processor.AnimationProcessor;
import com.fox.ysmu.geckolib3.core.processor.IBone;
import com.fox.ysmu.util.Keep;

@SuppressWarnings("rawtypes")
public interface IAnimatableModel<E> {
    /**
     * 获取当前的 tick
     *
     * @return 当前的 tick
     */
    @Keep
    default double getCurrentTick() {
        return System.nanoTime() / 1000000.0 / 50.0;
    }

    /**
     * 设置自定义动画
     *
     * @param animatable 对象
     * @param instanceId 实例 ID
     */
    @Keep
    default void setCustomAnimations(E animatable, int instanceId) {
        setCustomAnimations(animatable, instanceId, null);
    }

    /**
     * 设置自定义动画
     *
     * @param animatable     对象
     * @param instanceId     实例 ID
     * @param animationEvent 动画事件
     */
    @Keep
    default void setCustomAnimations(E animatable, int instanceId, AnimationEvent animationEvent) {
    }

    /**
     * 获取 Animation Processor
     *
     * @return AnimationProcessor
     */
    @Keep
    AnimationProcessor getAnimationProcessor();

    /**
     * 获取动画
     *
     * @param name       动画名称
     * @param animatable 对象
     * @return 动画
     */
    @Keep
    Animation getAnimation(String name, IAnimatable animatable);

    /**
     * 通过骨骼名获取 IBone
     *
     * @param boneName 骨骼名
     * @return IBone
     */
    @Keep
    default IBone getBone(String boneName) {
        IBone bone = getAnimationProcessor().getBone(boneName);
        if (bone == null) {
            throw new RuntimeException("Could not find bone: " + boneName);
        }
        return bone;
    }

    /**
     * molang 动画数据获取
     *
     * @param animatable 对象
     * @param seekTime   动画时间？？
     */
    @Keep
    void setMolangQueries(IAnimatable animatable, double seekTime);
}
