//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.fox.ysmu.geckolib3.core.builder;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.io.Serializable;
import java.util.Locale;

public interface ILoopType extends Serializable {
    /**
     * 从动画文件读取播放类型
     *
     * @param json json 文件
     * @return 播放类型
     */
    static ILoopType fromJson(JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) {
            return EDefaultLoopTypes.PLAY_ONCE;
        }
        JsonPrimitive primitive = json.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean() ? EDefaultLoopTypes.LOOP : EDefaultLoopTypes.PLAY_ONCE;
        }
        if (primitive.isString()) {
            String string = primitive.getAsString();
            if ("false".equalsIgnoreCase(string)) {
                return EDefaultLoopTypes.PLAY_ONCE;
            }
            if ("true".equalsIgnoreCase(string)) {
                return EDefaultLoopTypes.LOOP;
            }
            try {
                return EDefaultLoopTypes.valueOf(string.toUpperCase(Locale.ROOT));
            } catch (Exception ignore) {
            }
        }
        return EDefaultLoopTypes.PLAY_ONCE;
    }

    /**
     * 是否在动画结束后重复
     *
     * @return 是否在动画结束后重复
     */
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
