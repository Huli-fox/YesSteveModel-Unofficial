package com.fox.ysmu.mclib.math.functions.classic;

import com.fox.ysmu.mclib.math.IValue;
import com.fox.ysmu.mclib.math.functions.Function;
import com.fox.ysmu.util.Keep;

public class Ln extends Function {
    public Ln(IValue[] values, String name) throws Exception {
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
        return Math.log(this.getArg(0));
    }
}
