package com.fox.ysmu.geckolib3.core.processor;

import com.fox.ysmu.geckolib3.core.snapshot.BoneSnapshot;
import com.fox.ysmu.util.Keep;

public interface IBone {
    @Keep
    float getRotationX();

    @Keep
    void setRotationX(float value);

    @Keep
    float getRotationY();

    @Keep
    void setRotationY(float value);

    @Keep
    float getRotationZ();

    @Keep
    void setRotationZ(float value);

    @Keep
    float getPositionX();

    @Keep
    void setPositionX(float value);

    @Keep
    float getPositionY();

    @Keep
    void setPositionY(float value);

    @Keep
    float getPositionZ();

    @Keep
    void setPositionZ(float value);

    @Keep
    float getScaleX();

    @Keep
    void setScaleX(float value);

    @Keep
    float getScaleY();

    @Keep
    void setScaleY(float value);

    @Keep
    float getScaleZ();

    @Keep
    void setScaleZ(float value);

    @Keep
    float getPivotX();

    @Keep
    void setPivotX(float value);

    @Keep
    float getPivotY();

    @Keep
    void setPivotY(float value);

    @Keep
    float getPivotZ();

    @Keep
    void setPivotZ(float value);

    @Keep
    boolean isHidden();

    @Keep
    void setHidden(boolean hidden);

    @Keep
    boolean cubesAreHidden();

    @Keep
    boolean childBonesAreHiddenToo();

    @Keep
    void setCubesHidden(boolean hidden);

    @Keep
    void setHidden(boolean selfHidden, boolean skipChildRendering);

    @Keep
    void setModelRendererName(String modelRendererName);

    @Keep
    void saveInitialSnapshot();

    @Keep
    BoneSnapshot getInitialSnapshot();

    @Keep
    default BoneSnapshot saveSnapshot() {
        return new BoneSnapshot(this);
    }

    @Keep
    String getName();
}
