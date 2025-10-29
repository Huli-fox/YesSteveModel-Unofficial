package com.fox.ysmu.mclib.math.functions.utility;

import com.fox.ysmu.mclib.math.IValue;
import com.fox.ysmu.mclib.math.functions.Function;
import com.fox.ysmu.util.Keep;

public class HermiteBlend extends Function {
    public java.util.Random random;

    public HermiteBlend(IValue[] values, String name) throws Exception {
        super(values, name);
        this.random = new java.util.Random();
    }

    @Override
    @Keep
    public int getRequiredArguments() {
        return 1;
    }

    @Override
    @Keep
    public double get() {
        double min = Math.ceil(this.getArg(0));
        return Math.floor(3.0 * Math.pow(min, 2.0) - 2.0 * Math.pow(min, 3.0));
    }
}
