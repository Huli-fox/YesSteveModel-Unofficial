/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.fox.ysmu.geckolib3.core.builder;

import com.fox.ysmu.util.Keep;

import java.util.Objects;

public class RawAnimation {
    public String animationName;
    public ILoopType loopType;

    /**
     * 仅存储名称和播放循环类型的类
     */
    public RawAnimation(String animationName, ILoopType loop) {
        this.animationName = animationName;
        this.loopType = loop;
    }

    @Override
    @Keep
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RawAnimation animation)) {
            return false;
        }
        return animation.loopType == this.loopType && animation.animationName.equals(this.animationName);
    }

    @Override
    @Keep
    public int hashCode() {
        return Objects.hash(this.animationName, this.loopType);
    }
}
