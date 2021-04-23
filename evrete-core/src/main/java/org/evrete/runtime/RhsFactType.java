package org.evrete.runtime;

import org.evrete.api.*;

import java.util.Objects;

/**
 * <p>A runtime FactType representation used in right-hand-side iteration.</p>
 */
class RhsFactType {
    private final RuntimeFactType type;
    private final RhsFactGroup group;
    FactHandle handle;
    Object value;
    ReIterator<FactHandleVersioned> factIterator;
    private MemoryKey currentKey;
    private FactHandleVersioned currentFactHandle;

    RhsFactType(RuntimeFactType type, RhsFactGroup group) {
        this.type = type;
        this.group = group;
    }

    void resetState() {
        this.currentFactHandle = null;
        this.currentKey = null;
    }

    boolean setCurrentKey(MemoryKey key) {
        if (!Objects.equals(key, this.currentKey)) {
            this.currentKey = key;
            this.currentFactHandle = null;
            KeyMode mode = KeyMode.values()[key.getMetaValue()];
            this.factIterator = type.factIterator(mode, key);
        }
        return true;
    }

    boolean setCurrentFact(FactHandleVersioned v) {
        if (Objects.equals(v, currentFactHandle)) {
            // The same as previous, no need to query memory
            return true;
        } else {
            FactHandle handle = v.getHandle();
            FactRecord fact = type.get(handle);
            if (fact == null || fact.getVersion() != v.getVersion()) {
                System.out.println("!!!!!");
                return false;
            } else {
                this.currentFactHandle = v;
                this.handle = handle;
                this.value = fact.instance;
                return true;
            }
        }
    }

    @Override
    public String toString() {
        return "{" +
                "type=" + type +
                ", key=" + currentKey +
                ", fact=" + currentFactHandle +
                '}';
    }
}
