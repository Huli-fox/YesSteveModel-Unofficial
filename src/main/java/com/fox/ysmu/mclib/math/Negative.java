package com.fox.ysmu.mclib.math;

public class Negative implements IValue {
    public IValue value;

    public Negative(IValue value) {
        this.value = value;
    }

    @Override

    public double get() {
        return -this.value.get();
    }

    @Override

    public String toString() {
        return "-" + this.value.toString();
    }
}
