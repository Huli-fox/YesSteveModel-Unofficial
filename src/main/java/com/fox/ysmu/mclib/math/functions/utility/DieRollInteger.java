package com.fox.ysmu.mclib.math.functions.utility;

import com.fox.ysmu.mclib.math.IValue;
import com.fox.ysmu.mclib.math.functions.Function;
import com.fox.ysmu.util.Keep;

public class DieRollInteger extends Function {
    public java.util.Random random;

    public DieRollInteger(IValue[] values, String name) throws Exception {
        super(values, name);
        this.random = new java.util.Random();
    }

    @Override
    @Keep
    public int getRequiredArguments() {
        return 3;
    }

    @Override
    @Keep
    public double get() {
        double i = 0;
        double total = 0;
        while (i < this.getArg(0)) {
            total += (double) Math.round(this.getArg(1) + Math.random() * (this.getArg(2) - this.getArg(1)));
        }
        return total;
    }
}
