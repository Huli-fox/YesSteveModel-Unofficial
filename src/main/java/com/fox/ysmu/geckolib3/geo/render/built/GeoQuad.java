package com.fox.ysmu.geckolib3.geo.render.built;

import net.geckominecraft.util.math.Vec3i;
import net.minecraft.util.EnumFacing;
import com.fox.ysmu.geckolib3.geo.raw.pojo.FaceUv;

import java.io.Serializable;

public class GeoQuad implements Serializable {
    private static final long serialVersionUID = 42L;
    public GeoVertex[] vertices;
    public final Vec3i normal;
    public EnumFacing direction;
    public int uvRotation;

    public GeoQuad(GeoVertex[] verticesIn, float u1, float v1, float uSize, float vSize, int uvRotation, float texWidth,
                   float texHeight, Boolean mirrorIn, EnumFacing directionIn) {
        this.direction = directionIn;
        this.vertices = verticesIn;
        this.uvRotation = uvRotation;

        /*
         * u1 is the distance from the very left of the texture to where the uv region
         * starts v1 is the distance from the very top of the texture to where the uv
         * region starts u2 is the horizontal distance from u1 to where the uv region
         * ends v2 is the vertical distance from the v1 to where the uv region ends
         */

        float u2 = u1 + uSize;
        float v2 = v1 + vSize;

        float uWidth = u2 / texWidth;
        float vHeight = v2 / texHeight;
        u1 /= texWidth;
        v1 /= texHeight;

        int nx = directionIn.getFrontOffsetX();
        int ny = directionIn.getFrontOffsetY();
        int nz = directionIn.getFrontOffsetZ();

        boolean mirror = mirrorIn != null && mirrorIn;

        if (!mirror) {
            float temp = uWidth;
            uWidth = u1;
            u1 = temp;
        } else {
            nx *= -1;
        }

        float[] uvs = FaceUv.Rotation.fromValue(uvRotation).rotateUvs(u1, v1, uWidth, vHeight);
        vertices[0] = verticesIn[0].setTextureUV(uvs[0], uvs[1]);
        vertices[1] = verticesIn[1].setTextureUV(uvs[2], uvs[3]);
        vertices[2] = verticesIn[2].setTextureUV(uvs[4], uvs[5]);
        vertices[3] = verticesIn[3].setTextureUV(uvs[6], uvs[7]);
        this.normal = new Vec3i(nx, ny, nz);
    }

    public GeoQuad(GeoVertex[] verticesIn, double[] uvCoords, double[] uvSize, int uvRotation, float texWidth, float texHeight,
                   Boolean mirrorIn, EnumFacing directionIn) {
        this(verticesIn, (float) uvCoords[0], (float) uvCoords[1], (float) uvSize[0], (float) uvSize[1], uvRotation, texWidth,
            texHeight, mirrorIn, directionIn);
    }

    public GeoQuad(GeoVertex[] verticesIn, float u1, float v1, float uSize, float vSize, float texWidth,
                   float texHeight, Boolean mirrorIn, EnumFacing directionIn) {
        this(verticesIn, u1, v1, uSize, vSize, 0, texWidth, texHeight, mirrorIn, directionIn);
    }

    public GeoQuad(GeoVertex[] verticesIn, double[] uvCoords, double[] uvSize, float texWidth, float texHeight,
                   Boolean mirrorIn, EnumFacing directionIn) {
        this(verticesIn, (float) uvCoords[0], (float) uvCoords[1], (float) uvSize[0], (float) uvSize[1], 0, texWidth,
            texHeight, mirrorIn, directionIn);
    }
}
