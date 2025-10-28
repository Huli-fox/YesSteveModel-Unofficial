//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.fox.ysmu.geckolib3.core.builder;

import java.io.Serializable;

public interface ILoopType extends Serializable {
    boolean isRepeatingAfterEnd();
    static final long serialVersionUID = 42L;
    enum EDefaultLoopTypes implements ILoopType {
        LOOP(true),
        PLAY_ONCE,
        HOLD_ON_LAST_FRAME(true);

        private final boolean looping;

        EDefaultLoopTypes(boolean looping) {
            this.looping = looping;
        }

        EDefaultLoopTypes() {
            this(false);
        }

        public boolean isRepeatingAfterEnd() {
            return this.looping;
        }
    }
}
