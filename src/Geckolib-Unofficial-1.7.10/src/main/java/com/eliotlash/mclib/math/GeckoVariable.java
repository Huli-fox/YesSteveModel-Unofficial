package com.eliotlash.mclib.math;

import software.bernie.geckolib3.molang.MolangRegistrar;

public class GeckoVariable extends Variable {
    public GeckoVariable(String name, double value) {
        super(name, value);
    }

    public double get() {
        if (this != MolangRegistrar.getParser().getVariable(getName())) {
            return MolangRegistrar.getParser().getVariable(getName()).get();
        }
        return super.get();
    }
}
