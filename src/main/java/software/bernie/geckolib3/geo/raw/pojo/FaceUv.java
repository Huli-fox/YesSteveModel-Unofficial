package software.bernie.geckolib3.geo.raw.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Specifies the UV's for the face that stretches along the x and z axes, and
 * faces the -y axis
 * <p>
 * Specifies the UV's for the face that stretches along the z and y axes, and
 * faces the x axis
 * <p>
 * Specifies the UV's for the face that stretches along the x and y axes, and
 * faces the -z axis.
 * <p>
 * Specifies the UV's for the face that stretches along the x and y axes, and
 * faces the z axis
 * <p>
 * Specifies the UV's for the face that stretches along the x and z axes, and
 * faces the y axis
 * <p>
 * Specifies the UV's for the face that stretches along the z and y axes, and
 * faces the -x axis
 */
public class FaceUv {

    private String materialInstance;
    private double[] uv;
    private double[] uvSize;
    private Integer uvRotation;

    @JsonProperty("material_instance")
    public String getMaterialInstance() {
        return materialInstance;
    }

    @JsonProperty("material_instance")
    public void setMaterialInstance(String value) {
        this.materialInstance = value;
    }

    /**
     * Specifies the uv origin for the face. For this face, it is the upper-left
     * corner, when looking at the face with y being up.
     */
    @JsonProperty("uv")
    public double[] getUv() {
        return uv;
    }

    @JsonProperty("uv")
    public void setUv(double[] value) {
        this.uv = value;
    }

    /**
     * The face maps this many texels from the uv origin. If not specified, the box
     * dimensions are used instead.
     */
    @JsonProperty("uv_size")
    public double[] getUvSize() {
        return uvSize;
    }

    @JsonProperty("uv_size")
    public void setUvSize(double[] value) {
        this.uvSize = value;
    }

    @JsonProperty("uv_rotation")
    public Integer getUvRotation() {
        return uvRotation;
    }

    @JsonProperty("uv_rotation")
    public void setUvRotation(Integer value) {
        this.uvRotation = value;
    }

    public Rotation getRotationEnum() {
        return Rotation.fromValue(this.uvRotation == null ? 0 : this.uvRotation);
    }

    public enum Rotation {

        NONE,
        CLOCKWISE_90,
        CLOCKWISE_180,
        CLOCKWISE_270;

        public static Rotation fromValue(int value) {
            int index = ((value % 360) + 360) % 360 / 90;
            switch (index) {
                case 1:
                    return CLOCKWISE_90;
                case 2:
                    return CLOCKWISE_180;
                case 3:
                    return CLOCKWISE_270;
                default:
                    return NONE;
            }
        }

        public float[] rotateUvs(float u, float v, float u2, float v2) {
            switch (this) {
                case CLOCKWISE_90:
                    return new float[] { u2, v, u2, v2, u, v2, u, v };
                case CLOCKWISE_180:
                    return new float[] { u2, v2, u, v2, u, v, u2, v };
                case CLOCKWISE_270:
                    return new float[] { u, v2, u, v, u2, v, u2, v2 };
                default:
                    return new float[] { u, v, u2, v, u2, v2, u, v2 };
            }
        }
    }
}
