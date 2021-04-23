package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.FactHandleVersioned;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;

import java.util.Objects;

/**
 * <p>A runtime FactType representation used in right-hand-side iteration.</p>
 */
class RhsFactType {
    private final FactType type;
    private final RhsFactGroup group;
    private final TypeMemory typeMemory;
    FactHandle handle;
    Object value;
    ReIterator<FactHandleVersioned> factIterator;
    private MemoryKey currentKey;
    private FactHandleVersioned currentFactHandle;

    RhsFactType(SessionMemory memory, FactType type, RhsFactGroup group) {
        this.type = type;
        this.group = group;
        this.typeMemory = memory.get(type.type());
    }

    void resetState() {
        this.currentFactHandle = null;
        this.currentKey = null;
    }

    void setCurrentKey(MemoryKey key) {
        if (!Objects.equals(key, this.currentKey)) {
            this.currentKey = key;
            this.factIterator = group.factIterator(type, key);
            this.currentFactHandle = null;
        }
    }

    boolean setCurrentFact(FactHandleVersioned v) {
        if (Objects.equals(v, currentFactHandle)) {
            // The same as previous, no need to query memory
            return true;
        } else {
            FactHandle handle = v.getHandle();
            FactRecord fact = typeMemory.getStoredRecord(handle);
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
        return currentKey.toString();
    }
}
