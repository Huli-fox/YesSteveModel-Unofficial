package com.eliotlash.molang.functions;

import com.eliotlash.mclib.math.IValue;
import com.eliotlash.mclib.math.functions.classic.ASin;

public class AsinDegrees extends ASin {
    public AsinDegrees(IValue[] values, String name) throws Exception {
        super(values, name);
    }

    @Override
    public double get() {
        return super.get() / Math.PI * 180;
    }
}
