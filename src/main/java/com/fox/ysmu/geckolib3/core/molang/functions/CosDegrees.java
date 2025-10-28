package com.fox.ysmu.geckolib3.core.molang.functions;

import com.fox.ysmu.mclib.math.IValue;
import com.fox.ysmu.mclib.math.functions.Function;
import com.fox.ysmu.util.Keep;

public class CosDegrees extends Function {
    public CosDegrees(IValue[] values, String name) throws Exception {
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
        return Math.cos(this.getArg(0) / 180 * Math.PI);
    }
}
