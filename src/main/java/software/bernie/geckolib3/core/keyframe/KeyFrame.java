//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package software.bernie.geckolib3.core.keyframe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.bernie.geckolib3.core.easing.EasingType;

public class KeyFrame<T> implements Serializable {
    private static final long serialVersionUID = 42L;
    private Double length;
    private T startValue;
    private T endValue;
    public EasingType easingType;
    public List<Double> easingArgs;

    public KeyFrame(Double length, T startValue, T endValue) {
        this.easingType = EasingType.Linear;
        this.easingArgs = new ArrayList();
        this.length = length;
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public KeyFrame(Double length, T startValue, T endValue, EasingType easingType) {
        this.easingType = EasingType.Linear;
        this.easingArgs = new ArrayList();
        this.length = length;
        this.startValue = startValue;
        this.endValue = endValue;
        this.easingType = easingType;
    }

    public KeyFrame(Double length, T startValue, T endValue, EasingType easingType, List<Double> easingArgs) {
        this.easingType = EasingType.Linear;
        this.easingArgs = new ArrayList();
        this.length = length;
        this.startValue = startValue;
        this.endValue = endValue;
        this.easingType = easingType;
        this.easingArgs = easingArgs;
    }

    public Double getLength() {
        return this.length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public T getStartValue() {
        return this.startValue;
    }

    public void setStartValue(T startValue) {
        this.startValue = startValue;
    }

    public T getEndValue() {
        return this.endValue;
    }

    public void setEndValue(T endValue) {
        this.endValue = endValue;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.length, this.startValue, this.endValue});
    }

    public boolean equals(Object obj) {
        return obj instanceof KeyFrame && this.hashCode() == obj.hashCode();
    }
}
