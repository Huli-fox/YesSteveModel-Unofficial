package com.eliotlash.molang.functions;

import com.eliotlash.mclib.math.IValue;
import com.eliotlash.mclib.math.functions.classic.ATan2;

public class Atan2Degrees extends ATan2 {
    public Atan2Degrees(IValue[] values, String name) throws Exception {
        super(values, name);
    }

    @Override
    public double get() {
        return super.get() / Math.PI * 180;
    }
}
