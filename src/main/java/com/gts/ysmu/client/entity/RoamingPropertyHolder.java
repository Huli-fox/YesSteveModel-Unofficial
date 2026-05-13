package com.gts.ysmu.client.entity;

import com.gts.ysmu.molang.runtime.Struct;
import org.jetbrains.annotations.Nullable;

public interface RoamingPropertyHolder {
    @Nullable
    Struct getPropertyContainer();
}
