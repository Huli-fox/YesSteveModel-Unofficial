//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package software.bernie.geckolib3.core.builder;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import software.bernie.geckolib3.core.builder.ILoopType.EDefaultLoopTypes;
import software.bernie.geckolib3.core.keyframe.BoneAnimation;
import software.bernie.geckolib3.core.keyframe.EventKeyFrame;
import software.bernie.geckolib3.core.keyframe.ParticleEventKeyFrame;

public class Animation implements Serializable {
    private static final long serialVersionUID = 42L;
    public String animationName;
    public Double animationLength;
    public ILoopType loop;
    public List<BoneAnimation> boneAnimations;
    public List<EventKeyFrame<String>> soundKeyFrames;
    public List<ParticleEventKeyFrame> particleKeyFrames;
    public List<EventKeyFrame<String>> customInstructionKeyframes;

    public Animation() {
        this.loop = EDefaultLoopTypes.LOOP;
        this.soundKeyFrames = new ArrayList();
        this.particleKeyFrames = new ArrayList();
        this.customInstructionKeyframes = new ArrayList();
    }

    public static Animation copy(Animation animation) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(animation);
            objectOutputStream.flush();
            String serialized = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());

            byte[] data = Base64.getDecoder().decode(serialized);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            return (Animation) objectInputStream.readObject();
        } catch (Exception e) {
            return animation;
        }
    }
}
