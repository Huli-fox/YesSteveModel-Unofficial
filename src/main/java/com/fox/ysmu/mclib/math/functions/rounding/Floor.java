package com.fox.ysmu.mclib.math.functions.rounding;

import com.fox.ysmu.mclib.math.IValue;
import com.fox.ysmu.mclib.math.functions.Function;
import com.fox.ysmu.util.Keep;

public class Floor extends Function {
    public Floor(IValue[] values, String name) throws Exception {
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
        return Math.floor(this.getArg(0));
    }
}
