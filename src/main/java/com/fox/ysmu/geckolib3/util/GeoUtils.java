package com.fox.ysmu.geckolib3.util;

import net.minecraft.client.model.ModelRenderer;
import com.fox.ysmu.geckolib3.core.processor.IBone;

public class GeoUtils {
    public static void copyRotations(ModelRenderer from, IBone to) {
        to.setRotationX(-from.rotateAngleX);
        to.setRotationY(-from.rotateAngleY);
        to.setRotationZ(from.rotateAngleZ);
    }
}
