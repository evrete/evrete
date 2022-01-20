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

    private FactRecord(FactRecord prev, Object updatedFact) {
        this.instance = updatedFact;
        this.bucketsMask = Mask.addressMask();
        this.bucketsMask.or(prev.bucketsMask);
        this.version = prev.version + 1;
    }

    static FactRecord updated(FactRecord previous, Object updated) {
        return new FactRecord(previous, updated);
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

    @Override
    public String toString() {
        return "{obj=" + instance +
                ", ver=" + version +
                '}';
    }
}
