package com.fox.ysmu.geckolib3.particles.components;

public interface IComponentBase {
    public default int getSortingIndex() {
        return 0;
    }
}
