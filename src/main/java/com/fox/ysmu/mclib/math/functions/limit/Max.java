package com.fox.ysmu.mclib.math.functions.limit;

import com.fox.ysmu.mclib.math.IValue;
import com.fox.ysmu.mclib.math.functions.Function;
import com.fox.ysmu.util.Keep;

public class Max extends Function {
    public Max(IValue[] values, String name) throws Exception {
        super(values, name);
    }

    @Override
    @Keep
    public int getRequiredArguments() {
        return 2;
    }

    @Override
    @Keep
    public double get() {
        return Math.max(this.getArg(0), this.getArg(1));
    }
}
