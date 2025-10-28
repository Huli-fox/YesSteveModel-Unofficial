package com.fox.ysmu.geckolib3.file;

import com.fox.ysmu.geckolib3.core.builder.Animation;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public record AnimationFile(Map<String, Animation> animations) {
    public AnimationFile() {
        this(new Object2ObjectOpenHashMap<>());
    }

    public Animation getAnimation(String name) {
        return animations.get(name);
    }

    public Collection<Animation> getAllAnimations() {
        return this.animations.values();
    }

    public void putAnimation(String name, Animation animation) {
        this.animations.put(name, animation);
    }
}
//public class AnimationFile implements Serializable {
//    private static final long serialVersionUID = 42L;
//    private HashMap<String, Animation> animations = new HashMap<>();
//
//    public Animation getAnimation(String name) {
//        return animations.get(name);
//    }
//
//    public void putAnimation(String name, Animation animation) {
//        this.animations.put(name, animation);
//    }
//
//    public Collection<Animation> getAllAnimations() {
//        return this.animations.values();
//    }
//}
