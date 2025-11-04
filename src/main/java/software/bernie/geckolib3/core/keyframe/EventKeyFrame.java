//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package software.bernie.geckolib3.core.keyframe;

import java.io.Serializable;

public class EventKeyFrame<T> implements Serializable {

    private static final long serialVersionUID = 42L;
    private T eventData;
    private Double startTick;

    public EventKeyFrame(Double startTick, T eventData) {
        this.startTick = startTick;
        this.eventData = eventData;
    }

    public T getEventData() {
        return this.eventData;
    }

    public Double getStartTick() {
        return this.startTick;
    }
}
