package com.fox.ysmu.mclib.math;

import com.fox.ysmu.util.Keep;

public interface IValue {
    /**
     * 获取计算值或存储值
     */
    @Keep
    double get();
}
