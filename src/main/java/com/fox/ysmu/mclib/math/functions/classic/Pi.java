package com.fox.ysmu.mclib.math.functions.classic;

import com.fox.ysmu.mclib.math.IValue;
import com.fox.ysmu.mclib.math.functions.Function;
import com.fox.ysmu.util.Keep;

public class Pi extends Function {
    public Pi(IValue[] values, String name) throws Exception {
        super(values, name);
    }

    @Override
    @Keep
    public double get() {
        return 3.141592653589793d;
    }
}
