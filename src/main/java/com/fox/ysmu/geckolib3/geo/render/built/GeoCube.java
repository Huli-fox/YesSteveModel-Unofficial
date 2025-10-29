package com.fox.ysmu.geckolib3.geo.render.built;

import com.fox.ysmu.geckolib3.geo.raw.pojo.*;
import com.fox.ysmu.geckolib3.util.VectorUtils;
import net.minecraftforge.common.util.ForgeDirection; // TODO net.minecraft.util.EnumFacing?
import net.minecraft.util.Vec3;
import org.joml.Vector3f;

public class GeoCube {
    public GeoQuad[] quads = new GeoQuad[6];
    public Vector3f pivot;
    public Vector3f rotation;
    public Vector3f size = new Vector3f();
    public double inflate;
    public Boolean mirror;

    private GeoCube(double[] size) {
        if (size.length >= 3) {
            this.size.set((float) size[0], (float) size[1], (float) size[2]);
        }
    }

    public static GeoCube createFromPojoCube(Cube cubeIn, ModelProperties properties, Double boneInflate, Boolean mirror) {
        GeoCube cube = new GeoCube(cubeIn.getSize());

        UvUnion uvUnion = cubeIn.getUv();
        UvFaces faces = uvUnion.faceUV;
        boolean isBoxUV = uvUnion.isBoxUV;
        cube.mirror = cubeIn.getMirror();
        cube.inflate = cubeIn.getInflate() == null ? (boneInflate == null ? 0 : boneInflate) : cubeIn.getInflate() / 16;

        float textureHeight = properties.getTextureHeight().floatValue();
        float textureWidth = properties.getTextureWidth().floatValue();

        Vec3 size = VectorUtils.fromArray(cubeIn.getSize());
        Vec3 origin = VectorUtils.fromArray(cubeIn.getOrigin());
        origin = Vec3.createVectorHelper(-(origin.xCoord + size.xCoord) / 16, origin.yCoord / 16, origin.zCoord / 16);

        double newX = size.xCoord * 0.0625D;
        double newY = size.yCoord * 0.0625D;
        double newZ = size.zCoord * 0.0625D;
        size = Vec3.createVectorHelper(newX, newY, newZ);

        Vector3f rotation = VectorUtils.convertDoubleToFloat(VectorUtils.fromArray(cubeIn.getRotation()));
        rotation.mul(-1, -1, 1);

        rotation.set((float) Math.toRadians(rotation.x()), (float) Math.toRadians(rotation.y()), (float) Math.toRadians(rotation.z()));

        Vector3f pivot = VectorUtils.convertDoubleToFloat(VectorUtils.fromArray(cubeIn.getPivot()));
        pivot.mul(-1, 1, 1);

        cube.pivot = pivot;
        cube.rotation = rotation;

        GeoVertex P1 = new GeoVertex(origin.xCoord - cube.inflate, origin.yCoord - cube.inflate, origin.zCoord - cube.inflate);
        GeoVertex P2 = new GeoVertex(origin.xCoord - cube.inflate, origin.yCoord - cube.inflate,
                origin.zCoord + size.zCoord + cube.inflate);
        GeoVertex P3 = new GeoVertex(origin.xCoord - cube.inflate, origin.yCoord + size.yCoord + cube.inflate,
                origin.zCoord - cube.inflate);
        GeoVertex P4 = new GeoVertex(origin.xCoord - cube.inflate, origin.yCoord + size.yCoord + cube.inflate,
                origin.zCoord + size.zCoord + cube.inflate);
        GeoVertex P5 = new GeoVertex(origin.xCoord + size.xCoord + cube.inflate, origin.yCoord - cube.inflate,
                origin.zCoord - cube.inflate);
        GeoVertex P6 = new GeoVertex(origin.xCoord + size.xCoord + cube.inflate, origin.yCoord - cube.inflate,
                origin.zCoord + size.zCoord + cube.inflate);
        GeoVertex P7 = new GeoVertex(origin.xCoord + size.xCoord + cube.inflate, origin.yCoord + size.yCoord + cube.inflate,
                origin.zCoord - cube.inflate);
        GeoVertex P8 = new GeoVertex(origin.xCoord + size.xCoord + cube.inflate, origin.yCoord + size.yCoord + cube.inflate,
                origin.zCoord + size.zCoord + cube.inflate);

        GeoQuad quadWest;
        GeoQuad quadEast;
        GeoQuad quadNorth;
        GeoQuad quadSouth;
        GeoQuad quadUp;
        GeoQuad quadDown;

        if (!isBoxUV) {
            FaceUv west = faces.getWest();
            FaceUv east = faces.getEast();
            FaceUv north = faces.getNorth();
            FaceUv south = faces.getSouth();
            FaceUv up = faces.getUp();
            FaceUv down = faces.getDown();

            quadWest = west == null ? null
                    : new GeoQuad(new GeoVertex[]{P4, P3, P1, P2}, west.getUv(), west.getUvSize(), textureWidth,
                    textureHeight, cubeIn.getMirror(), ForgeDirection.WEST);
            quadEast = east == null ? null
                    : new GeoQuad(new GeoVertex[]{P7, P8, P6, P5}, east.getUv(), east.getUvSize(), textureWidth,
                    textureHeight, cubeIn.getMirror(), ForgeDirection.EAST);
            quadNorth = north == null ? null
                    : new GeoQuad(new GeoVertex[]{P3, P7, P5, P1}, north.getUv(), north.getUvSize(), textureWidth,
                    textureHeight, cubeIn.getMirror(), ForgeDirection.NORTH);
            quadSouth = south == null ? null
                    : new GeoQuad(new GeoVertex[]{P8, P4, P2, P6}, south.getUv(), south.getUvSize(), textureWidth,
                    textureHeight, cubeIn.getMirror(), ForgeDirection.SOUTH);
            quadUp = up == null ? null
                    : new GeoQuad(new GeoVertex[]{P4, P8, P7, P3}, up.getUv(), up.getUvSize(), textureWidth,
                    textureHeight, cubeIn.getMirror(), ForgeDirection.UP);
            quadDown = down == null ? null
                    : new GeoQuad(new GeoVertex[]{P1, P5, P6, P2}, down.getUv(), down.getUvSize(), textureWidth,
                    textureHeight, cubeIn.getMirror(), ForgeDirection.DOWN);

            if (Boolean.TRUE.equals(cubeIn.getMirror()) || Boolean.TRUE.equals(mirror)) {
                quadWest = west == null ? null
                        : new GeoQuad(new GeoVertex[]{P7, P8, P6, P5}, west.getUv(), west.getUvSize(), textureWidth,
                        textureHeight, cubeIn.getMirror(), ForgeDirection.WEST);
                quadEast = east == null ? null
                        : new GeoQuad(new GeoVertex[]{P4, P3, P1, P2}, east.getUv(), east.getUvSize(), textureWidth,
                        textureHeight, cubeIn.getMirror(), ForgeDirection.EAST);
                quadNorth = north == null ? null
                        : new GeoQuad(new GeoVertex[]{P3, P7, P5, P1}, north.getUv(), north.getUvSize(), textureWidth,
                        textureHeight, cubeIn.getMirror(), ForgeDirection.NORTH);
                quadSouth = south == null ? null
                        : new GeoQuad(new GeoVertex[]{P8, P4, P2, P6}, south.getUv(), south.getUvSize(), textureWidth,
                        textureHeight, cubeIn.getMirror(), ForgeDirection.SOUTH);
                quadUp = up == null ? null
                        : new GeoQuad(new GeoVertex[]{P1, P5, P6, P2}, up.getUv(), up.getUvSize(), textureWidth,
                        textureHeight, cubeIn.getMirror(), ForgeDirection.UP);
                quadDown = down == null ? null
                        : new GeoQuad(new GeoVertex[]{P4, P8, P7, P3}, down.getUv(), down.getUvSize(), textureWidth,
                        textureHeight, cubeIn.getMirror(), ForgeDirection.DOWN);
            }
        } else {
            double[] uv = cubeIn.getUv().boxUVCoords;
            Vec3 uvSize = VectorUtils.fromArray(cubeIn.getSize());
            uvSize = Vec3.createVectorHelper(Math.floor(uvSize.xCoord), Math.floor(uvSize.yCoord), Math.floor(uvSize.zCoord));

            quadWest = new GeoQuad(new GeoVertex[]{P4, P3, P1, P2},
                    new double[]{uv[0] + uvSize.zCoord + uvSize.xCoord, uv[1] + uvSize.zCoord}, new double[]{uvSize.zCoord, uvSize.yCoord},
                    textureWidth, textureHeight, cubeIn.getMirror(), ForgeDirection.WEST);
            quadEast = new GeoQuad(new GeoVertex[]{P7, P8, P6, P5}, new double[]{uv[0], uv[1] + uvSize.zCoord},
                    new double[]{uvSize.zCoord, uvSize.yCoord}, textureWidth, textureHeight, cubeIn.getMirror(), ForgeDirection.EAST);
            quadNorth = new GeoQuad(new GeoVertex[]{P3, P7, P5, P1},
                    new double[]{uv[0] + uvSize.zCoord, uv[1] + uvSize.zCoord}, new double[]{uvSize.xCoord, uvSize.yCoord},
                    textureWidth, textureHeight, cubeIn.getMirror(), ForgeDirection.NORTH);
            quadSouth = new GeoQuad(new GeoVertex[]{P8, P4, P2, P6},
                    new double[]{uv[0] + uvSize.zCoord + uvSize.xCoord + uvSize.zCoord, uv[1] + uvSize.zCoord},
                    new double[]{uvSize.xCoord, uvSize.yCoord}, textureWidth, textureHeight, cubeIn.getMirror(), ForgeDirection.SOUTH);
            quadUp = new GeoQuad(new GeoVertex[]{P4, P8, P7, P3}, new double[]{uv[0] + uvSize.zCoord, uv[1]},
                    new double[]{uvSize.xCoord, uvSize.zCoord}, textureWidth, textureHeight, cubeIn.getMirror(), ForgeDirection.UP);
            quadDown = new GeoQuad(new GeoVertex[]{P1, P5, P6, P2},
                    new double[]{uv[0] + uvSize.zCoord + uvSize.xCoord, uv[1] + uvSize.zCoord},
                    new double[]{uvSize.xCoord, -uvSize.zCoord}, textureWidth, textureHeight, cubeIn.getMirror(), ForgeDirection.DOWN);

            if (Boolean.TRUE.equals(cubeIn.getMirror()) || Boolean.TRUE.equals(mirror)) {
                quadWest = new GeoQuad(new GeoVertex[]{P7, P8, P6, P5},
                        new double[]{uv[0] + uvSize.zCoord + uvSize.xCoord, uv[1] + uvSize.zCoord},
                        new double[]{uvSize.zCoord, uvSize.yCoord}, textureWidth, textureHeight, cubeIn.getMirror(),
                        ForgeDirection.WEST);
                quadEast = new GeoQuad(new GeoVertex[]{P4, P3, P1, P2}, new double[]{uv[0], uv[1] + uvSize.zCoord},
                        new double[]{uvSize.zCoord, uvSize.yCoord}, textureWidth, textureHeight, cubeIn.getMirror(),
                        ForgeDirection.EAST);
                quadNorth = new GeoQuad(new GeoVertex[]{P3, P7, P5, P1},
                        new double[]{uv[0] + uvSize.zCoord, uv[1] + uvSize.zCoord}, new double[]{uvSize.xCoord, uvSize.yCoord},
                        textureWidth, textureHeight, cubeIn.getMirror(), ForgeDirection.NORTH);
                quadSouth = new GeoQuad(new GeoVertex[]{P8, P4, P2, P6},
                        new double[]{uv[0] + uvSize.zCoord + uvSize.xCoord + uvSize.zCoord, uv[1] + uvSize.zCoord},
                        new double[]{uvSize.xCoord, uvSize.yCoord}, textureWidth, textureHeight, cubeIn.getMirror(),
                        ForgeDirection.SOUTH);
                quadUp = new GeoQuad(new GeoVertex[]{P4, P8, P7, P3}, new double[]{uv[0] + uvSize.zCoord, uv[1]},
                        new double[]{uvSize.xCoord, uvSize.zCoord}, textureWidth, textureHeight, cubeIn.getMirror(), ForgeDirection.UP);
                quadDown = new GeoQuad(new GeoVertex[]{P1, P5, P6, P2},
                        new double[]{uv[0] + uvSize.zCoord + uvSize.xCoord, uv[1] + uvSize.zCoord},
                        new double[]{uvSize.xCoord, -uvSize.zCoord}, textureWidth, textureHeight, cubeIn.getMirror(),
                        ForgeDirection.DOWN);
            }
        }

        cube.quads[0] = quadWest;
        cube.quads[1] = quadEast;
        cube.quads[2] = quadNorth;
        cube.quads[3] = quadSouth;
        cube.quads[4] = quadUp;
        cube.quads[5] = quadDown;
        return cube;
    }
}
