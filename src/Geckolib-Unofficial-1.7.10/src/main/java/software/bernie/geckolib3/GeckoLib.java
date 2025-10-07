package software.bernie.geckolib3;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import software.bernie.geckolib3.resource.ResourceListener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class GeckoLib {
    public static final String ModID = "geckolib3";
    public static boolean hasInitialized;
    public static final String VERSION = "3.0.30";

    /**
     * This method MUST be called in your mod's constructor or during
     * onInitializeClient in fabric, otherwise models and animations won't be
     * loaded.
     */
    public static void initialize() {
        if (!hasInitialized) {
            callFuture(new FutureTask<>(() -> {
                if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
                    doOnlyOnClient();
                }
            }, null));
        }
        hasInitialized = true;
    }

    public static void callFuture(FutureTask<?> task) {
        try {
            task.run();
            task.get(); // Forces the exception to be thrown if any
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("[GeckoLib] Exception caught executing FutureTask: {}" + e.toString() + e);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void doOnlyOnClient() {
        ResourceListener.registerReloadListener();
    }
}
