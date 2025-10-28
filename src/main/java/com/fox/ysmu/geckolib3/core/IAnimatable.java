/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */
package com.fox.ysmu.geckolib3.core;

import com.fox.ysmu.geckolib3.core.manager.AnimationData;
import com.fox.ysmu.geckolib3.core.manager.AnimationFactory;

/**
 * This interface must be applied to any object that wants to be animated
 */
public interface IAnimatable {
	void registerControllers(AnimationData data);

	AnimationFactory getFactory();
}
