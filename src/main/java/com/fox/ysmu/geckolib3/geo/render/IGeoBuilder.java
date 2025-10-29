package com.fox.ysmu.geckolib3.geo.render;

import com.fox.ysmu.geckolib3.geo.raw.pojo.ModelProperties;
import com.fox.ysmu.geckolib3.geo.raw.tree.RawBoneGroup;
import com.fox.ysmu.geckolib3.geo.raw.tree.RawGeometryTree;
import com.fox.ysmu.geckolib3.geo.render.built.GeoBone;
import com.fox.ysmu.geckolib3.geo.render.built.GeoModel;
import com.fox.ysmu.util.Keep;

public interface IGeoBuilder {
    @Keep
    GeoModel constructGeoModel(RawGeometryTree geometryTree);

    @Keep
    GeoBone constructBone(RawBoneGroup bone, ModelProperties properties, GeoBone parent);
}
