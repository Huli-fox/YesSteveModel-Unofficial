package software.bernie.geckolib3.geo.exception;

import net.minecraft.util.ResourceLocation;

import java.io.Serial;

public class GeckoLibException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public GeckoLibException(ResourceLocation fileLocation, String message) {
        super(fileLocation + ": " + message);
    }

    public GeckoLibException(ResourceLocation fileLocation, String message, Throwable cause) {
        super(fileLocation + ": " + message, cause);
    }
}
