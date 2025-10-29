package com.fox.ysmu.mclib.math.functions.utility;

import com.fox.ysmu.mclib.math.IValue;
import com.fox.ysmu.mclib.math.functions.Function;
import com.fox.ysmu.mclib.utils.Interpolations;
import com.fox.ysmu.util.Keep;

public class LerpRotate extends Function {
    public LerpRotate(IValue[] values, String name) throws Exception {
        super(values, name);
    }

    @Override
    @Keep
    public int getRequiredArguments() {
        return 3;
    }

    @Override
    @Keep
    public double get() {
        return Interpolations.lerpYaw(this.getArg(0), this.getArg(1), this.getArg(2));
    }
}
