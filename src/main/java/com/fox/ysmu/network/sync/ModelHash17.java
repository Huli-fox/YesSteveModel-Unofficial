package com.fox.ysmu.network.sync;

import java.util.UUID;

public final class ModelHash17 {

    private final long hash1;
    private final long hash2;

    public ModelHash17(long hash1, long hash2) {
        this.hash1 = hash1;
        this.hash2 = hash2;
    }

    public long getHash1() {
        return hash1;
    }

    public long getHash2() {
        return hash2;
    }

    public UUID asUuid() {
        return new UUID(hash1, hash2);
    }

    public String toServerCacheFileName() {
        return String.format("%016x%016x", hash1, hash2);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ModelHash17)) {
            return false;
        }
        ModelHash17 other = (ModelHash17) obj;
        return hash1 == other.hash1 && hash2 == other.hash2;
    }

    @Override
    public int hashCode() {
        int result = (int) (hash1 ^ (hash1 >>> 32));
        result = 31 * result + (int) (hash2 ^ (hash2 >>> 32));
        return result;
    }
}
