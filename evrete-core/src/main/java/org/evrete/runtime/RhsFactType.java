package org.evrete.runtime;

import org.evrete.api.*;

import java.util.Objects;

/**
 * <p>A runtime FactType representation used in right-hand-side iteration.</p>
 */
class RhsFactType {
    private final RuntimeFactType type;
    FactHandle handle;
    FactRecord record;
    ReIterator<FactHandleVersioned> factIterator;
    private MemoryKey currentKey;
    private FactHandleVersioned currentFactHandle;

    RhsFactType(RuntimeFactType type) {
        this.type = type;
    }

    void resetState() {
        this.currentFactHandle = null;
        this.currentKey = null;
    }

    void setCurrentKey(MemoryKey key) {
        if (valueChanged(key)) {
            this.currentKey = key;
            this.currentFactHandle = null;
            KeyMode mode = KeyMode.values()[key.getMetaValue()];
            this.factIterator = type.factIterator(mode, key);
        }
    }

    private boolean valueChanged(MemoryKey key) {
        if (currentKey == null) {
            return true;
        } else if (currentKey == key) {
            return false;
        } else if (currentKey.getMetaValue() != key.getMetaValue()) {
            return true;
        } else return !Objects.equals(key, this.currentKey);
    }

    boolean setCurrentFact(FactHandleVersioned v) {
        if (Objects.equals(v, currentFactHandle)) {
            // The same as previous, no need to query memory
            return true;
        } else {
            FactHandle handle = v.getHandle();
            FactRecord rec = type.get(handle);
            if (rec == null || rec.getVersion() != v.getVersion()) {
                return false;
            } else {
                this.currentFactHandle = v;
                this.handle = handle;
                this.record = rec;
                return true;
            }
        }
    }

    @Override
    public String toString() {
        return "{" +
                "type=" + type +
                ", key=" + currentKey +
                //", fact=" + currentFactHandle +
                '}';
    }
}
