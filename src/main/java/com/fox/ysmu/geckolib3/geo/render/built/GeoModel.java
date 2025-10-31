package com.fox.ysmu.geckolib3.geo.render.built;

import com.fox.ysmu.geckolib3.geo.raw.pojo.ModelProperties;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GeoModel implements Serializable {
    private static final long serialVersionUID = 42L;
    public List<GeoBone> topLevelBones = new ArrayList<>();
    public List<GeoBone> leftHandBones = new ObjectArrayList<>();
    public List<GeoBone> rightHandBones = new ObjectArrayList<>();
    public List<GeoBone> elytraBones = new ObjectArrayList<>();
    @Nullable
    public GeoBone firstPersonHead = null;
    @Nullable
    public GeoBone firstPersonViewLocator = null;
    public ModelProperties properties;

    public boolean hasTopLevelBone(String name) {
        return topLevelBones.stream().anyMatch(bone -> bone.name.equals(name));
    }

    public Optional<GeoBone> getTopLevelBone(String name) {
        for (GeoBone bone : topLevelBones) {
            if (bone.name.equals(name)) {
                return Optional.of(bone);
            }
        }
        return Optional.empty();
    }

    public Optional<GeoBone> getBone(String name) {
        for (GeoBone bone : topLevelBones) {
            GeoBone optionalBone = getBoneRecursively(name, bone);
            if (optionalBone != null) {
                return Optional.of(optionalBone);
            }
        }
        return Optional.empty();
    }

    private GeoBone getBoneRecursively(String name, GeoBone bone) {
        if (bone.name.equals(name)) {
            return bone;
        }
        for (GeoBone childBone : bone.childBones) {
            if (childBone.name.equals(name)) {
                return childBone;
            }
            GeoBone optionalBone = getBoneRecursively(name, childBone);
            if (optionalBone != null) {
                return optionalBone;
            }
        }
        return null;
    }
}
