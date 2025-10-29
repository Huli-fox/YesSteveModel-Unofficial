package com.fox.ysmu.mclib.math.functions.classic;

import com.fox.ysmu.mclib.math.IValue;
import com.fox.ysmu.mclib.math.functions.Function;
import com.fox.ysmu.util.Keep;

public class Abs extends Function {
    public Abs(IValue[] values, String name) throws Exception {
        super(values, name);
    }

    @Override
    @Keep
    public int getRequiredArguments() {
        return 1;
    }

    @Override
    @Keep
    public double get() {
        return Math.abs(this.getArg(0));
    }
}
