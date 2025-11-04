package software.bernie.geckolib3.util;

public class MolangUtils {
    public static final float TRUE = 1;
    public static final float FALSE = 0;

    public static float normalizeTime(long timestamp) {
        return ((float) timestamp / 24000);
    }

    public static float booleanToFloat(boolean input) {
        return input ? 1.0F : 0.0F;
    }
}
