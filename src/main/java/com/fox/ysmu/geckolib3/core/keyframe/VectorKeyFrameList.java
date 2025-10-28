//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.fox.ysmu.geckolib3.core.keyframe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VectorKeyFrameList<T extends KeyFrame> implements Serializable {
    private static final long serialVersionUID = 42L;
    public List<T> xKeyFrames;
    public List<T> yKeyFrames;
    public List<T> zKeyFrames;

    public VectorKeyFrameList(List<T> XKeyFrames, List<T> YKeyFrames, List<T> ZKeyFrames) {
        this.xKeyFrames = XKeyFrames;
        this.yKeyFrames = YKeyFrames;
        this.zKeyFrames = ZKeyFrames;
    }

    public VectorKeyFrameList() {
        this.xKeyFrames = new ArrayList();
        this.yKeyFrames = new ArrayList();
        this.zKeyFrames = new ArrayList();
    }

    public double getLastKeyframeTime() {
        double xTime = 0.0;

        KeyFrame frame;
        for(Iterator var3 = this.xKeyFrames.iterator(); var3.hasNext(); xTime += frame.getLength()) {
            frame = (KeyFrame)var3.next();
        }

        double yTime = 0.0;

        for(Iterator var5 = this.yKeyFrames.iterator(); var5.hasNext(); yTime += frame.getLength()) {
            frame = (KeyFrame)var5.next();
        }

        double zTime = 0.0;

        for(Iterator var7 = this.zKeyFrames.iterator(); var7.hasNext(); zTime += frame.getLength()) {
            frame = (KeyFrame)var7.next();
        }

        return Math.max(xTime, Math.max(yTime, zTime));
    }
}
