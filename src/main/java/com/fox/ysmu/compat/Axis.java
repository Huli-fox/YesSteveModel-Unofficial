package com.fox.ysmu.compat;

import org.joml.Quaternionf;
import org.joml.Vector3f;

@FunctionalInterface
public interface Axis {
    Axis XN = (p_254437_) -> {
        return (new Quaternionf()).rotationX(-p_254437_);
    };
    Axis XP = (p_254466_) -> {
        return (new Quaternionf()).rotationX(p_254466_);
    };
    Axis YN = (p_254442_) -> {
        return (new Quaternionf()).rotationY(-p_254442_);
    };
    Axis YP = (p_254103_) -> {
        return (new Quaternionf()).rotationY(p_254103_);
    };
    Axis ZN = (p_254110_) -> {
        return (new Quaternionf()).rotationZ(-p_254110_);
    };
    Axis ZP = (p_253997_) -> {
        return (new Quaternionf()).rotationZ(p_253997_);
    };

    static Axis of(Vector3f p_254398_) {
        return (p_254401_) -> {
            return (new Quaternionf()).rotationAxis(p_254401_, p_254398_);
        };
    }

    Quaternionf rotation(float p_254545_);

    default Quaternionf rotationDegrees(float p_253800_) {
        return this.rotation(p_253800_ * ((float)Math.PI / 180F));
    }
}
