package com.fox.ysmu.mclib.math;

import com.fox.ysmu.util.Keep;

public class Ternary implements IValue {
    public IValue condition;
    public IValue ifTrue;
    public IValue ifFalse;

    public Ternary(IValue condition, IValue ifTrue, IValue ifFalse) {
        this.condition = condition;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
    }

    @Override
    @Keep
    public double get() {
        return this.condition.get() != 0 ? this.ifTrue.get() : this.ifFalse.get();
    }

    @Override
    @Keep
    public String toString() {
        return this.condition.toString() + " ? " + this.ifTrue.toString() + " : " + this.ifFalse.toString();
    }
}
