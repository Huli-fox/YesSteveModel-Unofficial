package com.eliotlash.mclib.math.functions.limit;

import com.eliotlash.mclib.math.IValue;
import com.eliotlash.mclib.math.functions.Function;

public class Clamp extends Function {

    public Clamp(IValue[] values, String name) throws Exception {
        super(values, name);
    }

    @Override

    public int getRequiredArguments() {
        return 3;
    }

    @Override

    public double get() {
        return clamp(this.getArg(0), this.getArg(1), this.getArg(2));
    }

    private double clamp(double p_14009_, double p_14010_, double p_14011_) {
        return p_14009_ < p_14010_ ? p_14010_ : Math.min(p_14009_, p_14011_);
    }
}
