package software.bernie.geckolib3.core.molang;

import java.util.HashMap;
import java.util.Map;

public final class MolangStringPool {

    public static final int EMPTY_ID = 0;

    private static final Map<String, Integer> IDS = new HashMap<>();
    private static final Map<Integer, String> VALUES = new HashMap<>();
    private static int nextId = 1;

    private MolangStringPool() {}

    public static synchronized int intern(String value) {
        if (value == null || value.isEmpty()) {
            return EMPTY_ID;
        }
        Integer existing = IDS.get(value);
        if (existing != null) {
            return existing;
        }
        int id = nextId++;
        IDS.put(value, id);
        VALUES.put(id, value);
        return id;
    }

    public static synchronized String get(int id) {
        return VALUES.get(id);
    }
}
