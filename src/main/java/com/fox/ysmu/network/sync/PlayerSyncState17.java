package com.fox.ysmu.network.sync;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

final class PlayerSyncState17 {

    final byte[] clientKey = new byte[56];
    byte[] key1;
    byte[] clientNextKey;
    int step;
    boolean versionAcknowledged;
    long lastTouchedMillis;
    final List<ServerModelData17> allowedModels = new ArrayList<>();

    PlayerSyncState17(SecureRandom random) {
        random.nextBytes(clientKey);
        touch();
    }

    void touch() {
        lastTouchedMillis = System.currentTimeMillis();
    }
}
