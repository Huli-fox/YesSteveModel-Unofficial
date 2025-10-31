package com.fox.ysmu.mclib.math.functions.classic;

import com.fox.ysmu.mclib.math.IValue;
import com.fox.ysmu.mclib.math.functions.Function;

public class Mod extends Function {
    public Mod(IValue[] values, String name) throws Exception {
        super(values, name);
    }

    @Override

    public int getRequiredArguments() {
        return 2;
    }

    @Override

    public double get() {
        return this.getArg(0) % this.getArg(1);
    }
}
