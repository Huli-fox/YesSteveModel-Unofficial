package com.fox.ysmu.mclib.math.functions.limit;

import com.fox.ysmu.mclib.math.IValue;
import com.fox.ysmu.mclib.math.functions.Function;
import com.fox.ysmu.util.Keep;
import net.minecraft.util.Mth;

public class Clamp extends Function {
    public Clamp(IValue[] values, String name) throws Exception {
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
        return Mth.clamp(this.getArg(0), this.getArg(1), this.getArg(2));
    }
}
