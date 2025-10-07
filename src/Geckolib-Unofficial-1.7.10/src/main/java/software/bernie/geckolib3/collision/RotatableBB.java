package software.bernie.geckolib3.collision;

import net.minecraft.util.AxisAlignedBB;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoVertex;
import software.bernie.geckolib3.util.MatrixStack;

import javax.vecmath.Vector4f;

public class RotatableBB extends AxisAlignedBB {
    public RotatableBB(GeoCube cube, MatrixStack stack) {
        super(0, 0, 0, 0, 0, 0);
        int i = 0;
        double[][] vertices = new double[8][];
        for (GeoVertex vertex : cube.quads[4].vertices) {
            Vector4f vector4f = new Vector4f(vertex.position.x, vertex.position.y, vertex.position.z,
                1.0F);
            stack.getModelMatrix().transform(vector4f);
            vertices[i] = new double[]{vector4f.x, vector4f.y, vector4f.z};
            i++;
        }
        for (GeoVertex vertex : cube.quads[5].vertices) {
            Vector4f vector4f = new Vector4f(vertex.position.x, vertex.position.y, vertex.position.z,
                1.0F);
            stack.getModelMatrix().transform(vector4f);
            vertices[i] = new double[]{vector4f.x, vector4f.y, vector4f.z};
            i++;
        }
        setupVertices(vertices);
    }

    public RotatableBB(double[][] vertices) {
        super(0, 0, 0, 0, 0, 0);
        setupVertices(vertices);
    }

    public void setupVertices(double[][] vertices) {
        if (vertices.length != 8) return;

        minX = findMin(vertices, 0);
        minY = findMin(vertices, 1);
        minZ = findMin(vertices, 2);

        maxX = findMax(vertices, 0);
        maxY = findMax(vertices, 1);
        maxZ = findMax(vertices, 2);
    }

    public double findMax(double[][] vertices, int secondIndex) {
        double max = vertices[0][secondIndex];
        for (int i = 0; i < vertices.length; i++) {
            max = Math.max(max, vertices[i][secondIndex]);
        }
        return max;
    }

    public double findMin(double[][] vertices, int secondIndex) {
        double min = vertices[0][secondIndex];
        for (int i = 0; i < vertices.length; i++) {
            min = Math.min(min, vertices[i][secondIndex]);
        }
        return min;
    }
}
