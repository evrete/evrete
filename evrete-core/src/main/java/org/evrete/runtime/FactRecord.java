package org.evrete.runtime;

import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

class FactRecord {
    final Object instance;
    private int version = 0;
    private final Mask<MemoryAddress> bucketsMask;

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

    void updateVersion(int newVersion) {
        this.version = newVersion;
    }

    FactRecord nextVersion() {
        FactRecord updated = new FactRecord(this.instance);
        updated.version = this.version + 1;
        return updated;
    }

    @Override
    public String toString() {
        return "{obj=" + instance +
                ", ver=" + version +
                '}';
    }
}
