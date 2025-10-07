package software.bernie.geckolib3.molang;

import com.eliotlash.mclib.math.GeckoVariable;
import com.eliotlash.molang.MolangParser;

public class MolangRegistrar {
    private static MolangParser instance;

    public static MolangParser getParser() {
        if (instance == null) {
            instance = new MolangParser();
            MolangRegistrar.registerVars(instance);
        }
        return instance;
    }

    public static void registerVars(MolangParser parser) {
        parser.register(new GeckoVariable("query.anim_time", 0));
        parser.register(new GeckoVariable("query.actor_count", 0));
        parser.register(new GeckoVariable("query.health", 0));
        parser.register(new GeckoVariable("query.max_health", 0));
        parser.register(new GeckoVariable("query.distance_from_camera", 0));
        parser.register(new GeckoVariable("query.yaw_speed", 0));
        parser.register(new GeckoVariable("query.is_in_water_or_rain", 0));
        parser.register(new GeckoVariable("query.is_in_water", 0));
        parser.register(new GeckoVariable("query.is_on_ground", 0));
        parser.register(new GeckoVariable("query.time_of_day", 0));
        parser.register(new GeckoVariable("query.is_on_fire", 0));
        parser.register(new GeckoVariable("query.ground_speed", 0));
    }
}
