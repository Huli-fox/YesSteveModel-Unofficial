package com.gts.ysmu.client.entity;

import com.gts.ysmu.client.animation.AnimationTracker;
import org.jetbrains.annotations.NotNull;

public interface IPreviewAnimatable {
    @NotNull
    AnimationTracker getAnimationStateMachine();

    void setCustomAnimationActive(boolean active);
}
