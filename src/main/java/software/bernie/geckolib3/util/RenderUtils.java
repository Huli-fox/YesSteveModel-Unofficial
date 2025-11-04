package software.bernie.geckolib3.util;

import com.fox.ysmu.compat.QuatJ2L;
import org.lwjgl.util.vector.Quaternion;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import com.fox.ysmu.compat.Axis;
import net.geckominecraft.client.renderer.GlStateManager;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RenderUtils {
    public static void translateMatrixToBone(GeoBone bone) {
        GlStateManager.translate(-bone.getPositionX() / 16f, bone.getPositionY() / 16f, bone.getPositionZ() / 16f);
    }

    public static void rotateMatrixAroundBone(GeoBone bone) {
        if (bone.getRotationZ() != 0.0F) {
            GlStateManager.rotate(j2l(Axis.ZP.rotation(bone.getRotationZ())));
        }
        if (bone.getRotationY() != 0.0F) {
            GlStateManager.rotate(j2l(Axis.YP.rotation(bone.getRotationY())));
        }
        if (bone.getRotationX() != 0.0F) {
            GlStateManager.rotate(j2l(Axis.XP.rotation(bone.getRotationX())));
        }
    }

    public static void rotateMatrixAroundCube(GeoCube cube) {
        Vector3f rotation = new Vector3f(cube.rotation.x, cube.rotation.y, cube.rotation.z);
        GlStateManager.rotate(j2l(new Quaternionf().rotationXYZ(0, 0, rotation.z())));
        GlStateManager.rotate(j2l(new Quaternionf().rotationXYZ(0, (float) rotation.y(), 0)));
        GlStateManager.rotate(j2l(new Quaternionf().rotationXYZ(rotation.x(), 0, 0)));
    }

    public static void scaleMatrixForBone(GeoBone bone) {
        GlStateManager.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
    }

    public static void translateToPivotPoint(GeoCube cube) {
        Vector3f pivot = new Vector3f(cube.pivot.x, cube.pivot.y, cube.pivot.z);
        GlStateManager.translate(pivot.x() / 16f, pivot.y() / 16f, pivot.z() / 16f);
    }

    public static void translateToPivotPoint(GeoBone bone) {
        GlStateManager.translate(bone.rotationPointX / 16f, bone.rotationPointY / 16f, bone.rotationPointZ / 16f);
    }

    public static void translateAwayFromPivotPoint(GeoCube cube) {
        Vector3f pivot = new Vector3f(cube.pivot.x, cube.pivot.y, cube.pivot.z);
        GlStateManager.translate(-pivot.x() / 16f, -pivot.y() / 16f, -pivot.z() / 16f);
    }

    public static void translateAwayFromPivotPoint(GeoBone bone) {
        GlStateManager.translate(-bone.rotationPointX / 16f, -bone.rotationPointY / 16f, -bone.rotationPointZ / 16f);
    }

    public static void translateAndRotateMatrixForBone(GeoBone bone) {
        translateToPivotPoint(bone);
        rotateMatrixAroundBone(bone);
    }

    public static void prepMatrixForBone(GeoBone bone) {
        translateMatrixToBone(bone);
        translateToPivotPoint(bone);
        rotateMatrixAroundBone(bone);
        scaleMatrixForBone(bone);
        translateAwayFromPivotPoint(bone);
    }

    public static Matrix4f invertAndMultiplyMatrices(Matrix4f baseMatrix, Matrix4f inputMatrix) {
        inputMatrix = new Matrix4f(inputMatrix);
        inputMatrix.invert();
        inputMatrix.mul(baseMatrix);
        return inputMatrix;
    }

    private static Quaternion j2l(Quaternionf jomlQuat) {
        return QuatJ2L.j2l(jomlQuat);
    }
}
