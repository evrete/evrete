package org.evrete.runtime;

import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

public class FactRecord {
    final Object instance;
    private final Mask<MemoryAddress> bucketsMask;
    private int version = 0;

    FactRecord(Object instance) {
        this.instance = instance;
        this.bucketsMask = Mask.addressMask();
    }

    void markLocation(MemoryAddress address) {
        this.bucketsMask.set(address);
    }

    public Mask<MemoryAddress> getBucketsMask() {
        return bucketsMask;
    }

    public int getVersion() {
        return version;
    }

    public void updateVersion(int newVersion) {
        this.version = newVersion;
    }

    @Override
    public String toString() {
        return "{obj=" + instance +
                ", ver=" + version +
                '}';
    }
}
