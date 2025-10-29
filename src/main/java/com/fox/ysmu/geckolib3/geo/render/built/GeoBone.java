package com.fox.ysmu.geckolib3.geo.render.built;

import com.fox.ysmu.geckolib3.core.processor.IBone;
import com.fox.ysmu.geckolib3.core.snapshot.BoneSnapshot;
import com.fox.ysmu.util.Keep;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public class GeoBone implements IBone {
    public GeoBone parent;
    public List<GeoBone> childBones = new ObjectArrayList<>();
    public List<GeoCube> childCubes = new ObjectArrayList<>();
    public String name;
    public Boolean mirror;
    public Double inflate;
    public Boolean dontRender;
    public boolean isHidden;
    public boolean areCubesHidden = false;
    public boolean hideChildBonesToo;
    /**
     * 我也不知道这个参数有啥用，但是 json 里面就有
     */
    public Boolean reset;
    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;
    public Matrix4f rotMat;
    private BoneSnapshot initialSnapshot;
    private float scaleX = 1;
    private float scaleY = 1;
    private float scaleZ = 1;
    private float positionX;
    private float positionY;
    private float positionZ;
    private float rotateX;
    private float rotateY;
    private float rotateZ;
    private Matrix4f modelSpaceXform;
    private Matrix4f localSpaceXform;
    private Matrix4f worldSpaceXform;
    private Matrix3f worldSpaceNormal;
    private boolean trackXform;

    public GeoBone() {
        modelSpaceXform = new Matrix4f();
        modelSpaceXform.identity();
        localSpaceXform = new Matrix4f();
        localSpaceXform.identity();
        worldSpaceXform = new Matrix4f();
        worldSpaceXform.identity();
        worldSpaceNormal = new Matrix3f();
        worldSpaceNormal.identity();
        trackXform = false;
        rotMat = null;
    }

    @Override
    @Keep
    public void setModelRendererName(String modelRendererName) {
        this.name = modelRendererName;
    }

    @Override
    @Keep
    public void saveInitialSnapshot() {
        if (this.initialSnapshot == null) {
            this.initialSnapshot = new BoneSnapshot(this, true);
        }
    }

    @Override
    @Keep
    public BoneSnapshot getInitialSnapshot() {
        return this.initialSnapshot;
    }


    @Override
    @Keep
    public String getName() {
        return this.name;
    }

    @Override
    @Keep
    public float getRotationX() {
        return rotateX;
    }

    @Override
    @Keep
    public void setRotationX(float value) {
        this.rotateX = value;
    }

    @Override
    @Keep
    public float getRotationY() {
        return rotateY;
    }

    @Override
    @Keep
    public void setRotationY(float value) {
        this.rotateY = value;
    }

    @Override
    @Keep
    public float getRotationZ() {
        return rotateZ;
    }

    @Override
    @Keep
    public void setRotationZ(float value) {
        this.rotateZ = value;
    }

    @Override
    @Keep
    public float getPositionX() {
        return positionX;
    }

    @Override
    @Keep
    public void setPositionX(float value) {
        this.positionX = value;
    }

    @Override
    @Keep
    public float getPositionY() {
        return positionY;
    }

    @Override
    @Keep
    public void setPositionY(float value) {
        this.positionY = value;
    }

    @Override
    @Keep
    public float getPositionZ() {
        return positionZ;
    }

    @Override
    @Keep
    public void setPositionZ(float value) {
        this.positionZ = value;
    }

    @Override
    @Keep
    public float getScaleX() {
        return scaleX;
    }

    @Override
    @Keep
    public void setScaleX(float value) {
        this.scaleX = value;
    }

    @Override
    @Keep
    public float getScaleY() {
        return scaleY;
    }

    @Override
    @Keep
    public void setScaleY(float value) {
        this.scaleY = value;
    }

    @Override
    @Keep
    public float getScaleZ() {
        return scaleZ;
    }

    @Override
    @Keep
    public void setScaleZ(float value) {
        this.scaleZ = value;
    }

    @Override
    @Keep
    public boolean isHidden() {
        return this.isHidden;
    }

    @Override
    @Keep
    public void setHidden(boolean hidden) {
        this.setHidden(hidden, hidden);
    }

    @Override
    @Keep
    public float getPivotX() {
        return this.rotationPointX;
    }

    @Override
    @Keep
    public void setPivotX(float value) {
        this.rotationPointX = value;
    }

    @Override
    @Keep
    public float getPivotY() {
        return this.rotationPointY;
    }

    @Override
    @Keep
    public void setPivotY(float value) {
        this.rotationPointY = value;
    }

    @Override
    @Keep
    public float getPivotZ() {
        return this.rotationPointZ;
    }

    @Override
    @Keep
    public void setPivotZ(float value) {
        this.rotationPointZ = value;
    }

    @Override
    @Keep
    public boolean cubesAreHidden() {
        return areCubesHidden;
    }

    @Override
    @Keep
    public boolean childBonesAreHiddenToo() {
        return hideChildBonesToo;
    }

    @Override
    @Keep
    public void setCubesHidden(boolean hidden) {
        this.areCubesHidden = hidden;
    }

    @Override
    @Keep
    public void setHidden(boolean selfHidden, boolean skipChildRendering) {
        this.isHidden = selfHidden;
        this.hideChildBonesToo = skipChildRendering;
    }

    public GeoBone getParent() {
        return parent;
    }

    public boolean isTrackingXform() {
        return trackXform;
    }

    public void setTrackXform(boolean trackXform) {
        this.trackXform = trackXform;
    }

    public void setModelSpaceXform(Matrix4f modelSpaceXform) {
        this.modelSpaceXform.get(modelSpaceXform);
    }

    public void setLocalSpaceXform(Matrix4f localSpaceXform) {
        this.localSpaceXform.get(localSpaceXform);
    }

    public void setWorldSpaceXform(Matrix4f worldSpaceXform) {
        this.worldSpaceXform.get(worldSpaceXform);
    }

    public void addPositionX(float x) {
        setPositionX(getPositionX() + x);
    }

    public void addPositionY(float y) {
        setPositionY(getPositionY() + y);
    }

    public void addPositionZ(float z) {
        setPositionZ(getPositionZ() + z);
    }

    public void setPosition(float x, float y, float z) {
        setPositionX(x);
        setPositionY(y);
        setPositionZ(z);
    }

    public Vector3d getPosition() {
        return new Vector3d(getPositionX(), getPositionY(), getPositionZ());
    }

    public void setPosition(Vector3d vec) {
        setPosition((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public void addRotation(Vector3d vec) {
        addRotation((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public void addRotation(float x, float y, float z) {
        addRotationX(x);
        addRotationY(y);
        addRotationZ(z);
    }

    public void addRotationX(float x) {
        setRotationX(getRotationX() + x);
    }

    public void addRotationY(float y) {
        setRotationY(getRotationY() + y);
    }

    public void addRotationZ(float z) {
        setRotationZ(getRotationZ() + z);
    }

    public void setRotation(float x, float y, float z) {
        setRotationX(x);
        setRotationY(y);
        setRotationZ(z);
    }

    public Vector3d getRotation() {
        return new Vector3d(getRotationX(), getRotationY(), getRotationZ());
    }

    public void setRotation(Vector3d vec) {
        setRotation((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public void multiplyScale(Vector3d vec) {
        multiplyScale((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public void multiplyScale(float x, float y, float z) {
        setScaleX(getScaleX() * x);
        setScaleY(getScaleY() * y);
        setScaleZ(getScaleZ() * z);
    }

    public void setScale(float x, float y, float z) {
        setScaleX(x);
        setScaleY(y);
        setScaleZ(z);
    }

    public Vector3d getScale() {
        return new Vector3d(getScaleX(), getScaleY(), getScaleZ());
    }

    public void setScale(Vector3d vec) {
        setScale((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public void addRotationOffsetFromBone(GeoBone source) {
        setRotationX(getRotationX() + source.getRotationX() - source.getInitialSnapshot().rotationValueX);
        setRotationY(getRotationY() + source.getRotationY() - source.getInitialSnapshot().rotationValueY);
        setRotationZ(getRotationZ() + source.getRotationZ() - source.getInitialSnapshot().rotationValueZ);
    }
}
