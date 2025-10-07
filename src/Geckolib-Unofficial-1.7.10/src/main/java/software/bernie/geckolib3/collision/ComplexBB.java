package software.bernie.geckolib3.collision;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.util.MatrixStack;

import java.util.ArrayList;
import java.util.Arrays;

public class ComplexBB extends AxisAlignedBB {
    public ArrayList<AxisAlignedBB> boundingBoxes;
    public static MatrixStack MATRIX_STACK = new MatrixStack();

    public ComplexBB(AxisAlignedBB[] bbs) {
        super(0, 0, 0, 0, 0, 0);
        boundingBoxes = new ArrayList<>();
        boundingBoxes.addAll(Arrays.asList(bbs));
        setupSize();
    }

    public ComplexBB(GeoModel model, double xOff, double yOff, double zOff) {
        super(0, 0, 0, 0, 0, 0);
        boundingBoxes = new ArrayList<>();
        MATRIX_STACK.push();
        MATRIX_STACK.translate((float) xOff, (float) yOff, (float) zOff);
        for (GeoBone group : model.topLevelBones) {
            walkBonesRecursively(group, xOff, yOff, zOff);
        }
        MATRIX_STACK.pop();
        setupSize();
    }

    public void setupSize() {
        if (boundingBoxes.size() == 0) return;
        minX = boundingBoxes.get(0).minX;
        minY = boundingBoxes.get(0).minY;
        minZ = boundingBoxes.get(0).minZ;
        maxX = boundingBoxes.get(0).maxX;
        maxY = boundingBoxes.get(0).maxY;
        maxZ = boundingBoxes.get(0).maxZ;


        for (AxisAlignedBB bb : boundingBoxes) {
            minX = Math.min(minX, bb.minX);
            minY = Math.min(minY, bb.minY);
            minZ = Math.min(minZ, bb.minZ);

            maxX = Math.max(maxX, bb.maxX);
            maxY = Math.max(maxY, bb.maxY);
            maxZ = Math.max(maxZ, bb.maxZ);
        }
    }

    public void walkBonesRecursively(GeoBone bone, double xOff, double yOff, double zOff) {
        MATRIX_STACK.push();

        MATRIX_STACK.translate(bone);
        MATRIX_STACK.moveToPivot(bone);
        MATRIX_STACK.rotate(bone);
        MATRIX_STACK.scale(bone);
        MATRIX_STACK.moveBackFromPivot(bone);
        if (!bone.isHidden()) {
            for (GeoCube cube : bone.childCubes) {
                MATRIX_STACK.push();
                MATRIX_STACK.moveToPivot(cube);
                MATRIX_STACK.rotate(cube);
                MATRIX_STACK.moveBackFromPivot(cube);
                //MATRIX_STACK.translate((float) xOff, (float) yOff, (float) zOff);
                boundingBoxes.add(new RotatableBB(cube, MATRIX_STACK));
                MATRIX_STACK.pop();
            }
        }
        if (!bone.childBonesAreHiddenToo()) {
            for (GeoBone childBone : bone.childBones) {
                walkBonesRecursively(childBone, xOff, yOff, zOff);
            }
        }
        MATRIX_STACK.pop();
    }

    public ComplexBB getOffsetBoundingBox(double p_72325_1_, double p_72325_3_, double p_72325_5_) {
        ComplexBB copy = copy();
        copy.offset(p_72325_1_, p_72325_3_, p_72325_5_);
        return copy;
    }

    public ComplexBB expand(double p_72314_1_, double p_72314_3_, double p_72314_5_) {
        ComplexBB copy = copy();
        copy.minX -= p_72314_1_;
        copy.minY -= p_72314_3_;
        copy.minZ -= p_72314_5_;
        copy.maxX += p_72314_1_;
        copy.maxY += p_72314_3_;
        copy.maxZ += p_72314_5_;
        for (AxisAlignedBB bb : boundingBoxes) {
            bb.expand(p_72314_1_, p_72314_3_, p_72314_5_);
        }
        return copy;
    }

    public ComplexBB contract(double p_72314_1_, double p_72314_3_, double p_72314_5_) {
        ComplexBB copy = copy();
        copy.minX += p_72314_1_;
        copy.minY += p_72314_3_;
        copy.minZ += p_72314_5_;
        copy.maxX -= p_72314_1_;
        copy.maxY -= p_72314_3_;
        copy.maxZ -= p_72314_5_;
        for (AxisAlignedBB bb : boundingBoxes) {
            copy.contract(p_72314_1_, p_72314_3_, p_72314_5_);
        }
        return copy;
    }

    @Override
    public boolean intersectsWith(AxisAlignedBB p_72326_1_) {
        if (p_72326_1_ == null) return false;
        boolean flag = false;
        for (AxisAlignedBB bb : boundingBoxes) {
            flag |= bb.intersectsWith(p_72326_1_);
        }
        return flag;
    }

    public ComplexBB offset(double p_72317_1_, double p_72317_3_, double p_72317_5_) {
        super.offset(p_72317_1_, p_72317_3_, p_72317_5_);
        for (AxisAlignedBB bb : boundingBoxes) {
            bb.offset(p_72317_1_, p_72317_3_, p_72317_5_);
        }
        return this;
    }

    @Override
    public boolean isVecInside(Vec3 p_72318_1_) {
        if (p_72318_1_ == null) return false;
        boolean flag = false;
        for (AxisAlignedBB bb : boundingBoxes) {
            flag |= bb.isVecInside(p_72318_1_);
        }
        return flag;
    }

    public ComplexBB copy() {
        AxisAlignedBB[] bbs = new AxisAlignedBB[boundingBoxes.size()];
        for (int i = 0; i < boundingBoxes.size(); i++) {
            bbs[i] = boundingBoxes.get(i).copy();
        }
        return new ComplexBB(bbs);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("complexbb{");
        for (AxisAlignedBB bb : boundingBoxes) {
            stringBuilder.append(bb.toString()).append(',');
        }
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
