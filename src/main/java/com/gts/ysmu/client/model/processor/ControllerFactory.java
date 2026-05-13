package com.gts.ysmu.client.model.processor;

import com.gts.ysmu.geckolib3.core.controller.IAnimationController;
import com.gts.ysmu.client.entity.GeoEntity;

import java.util.function.Consumer;

public interface ControllerFactory<T extends GeoEntity<?>> {
    void create(T entity, Consumer<IAnimationController<T>> consumer);
}
