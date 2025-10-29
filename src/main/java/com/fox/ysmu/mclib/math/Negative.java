package com.fox.ysmu.mclib.math;

import com.fox.ysmu.util.Keep;

public class Negative implements IValue {
    public IValue value;

    public Negative(IValue value) {
        this.value = value;
    }

    @Override
    @Keep
    public double get() {
        return -this.value.get();
    }

    @Override
    @Keep
    public String toString() {
        return "-" + this.value.toString();
    }
}
