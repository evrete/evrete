package org.evrete.runtime.rete;

import org.evrete.api.ReteMemory;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.spi.MemoryScope;
import org.evrete.runtime.FactFieldValues;
import org.evrete.runtime.PreHashed;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

public class ConditionMemory implements ReteMemory<ConditionMemory.MemoryEntry> {
    private final HashedCollection main = new HashedCollection();
    private final HashedCollection delta = new HashedCollection();

    void deleteAll(Predicate<MemoryEntry> predicate) {
        this.main.delete(predicate);
        this.delta.delete(predicate);
    }

    void saveNewEntry(MemoryScope destination, MemoryEntry entry) {
        if (destination == MemoryScope.DELTA) {
            delta.add(entry);
        } else if (destination == MemoryScope.MAIN) {
            main.add(entry);
        } else {
            throw new IllegalArgumentException("Unknown scope: " + destination);
        }
    }

    public int size(MemoryScope scope) {
        if (scope == MemoryScope.DELTA) {
            return delta.size();
        } else if (scope == MemoryScope.MAIN) {
            return main.size();
        } else {
            throw new IllegalArgumentException("Unknown scope: " + scope);
        }
    }

    @Override
    public void commit() {
        Iterator<MemoryEntry> iterator = delta.iterator();
        while (iterator.hasNext()) {
            MemoryEntry entry = iterator.next();
            main.add(entry.toMainScope());
            iterator.remove();
        }
    }

    @Override
    public void clear() {
        this.main.reset();
        this.delta.reset();
    }

    void clearDeltaMemory() {
        this.delta.reset();
    }

    @Override
    public String toString() {
        return "{" +
                "main=" + main.size() +
                ", delta=" + delta.size() +
                '}';
    }

    public Iterator<MemoryEntry> iterator(MemoryScope scope) {
        switch (scope) {
            case DELTA:
                return delta.iterator();
            case MAIN:
                return main.iterator();
            default:
                throw new IllegalStateException("Unknown scope " + scope);
        }
    }

    /**
     * A wrapper for an array of {@link FactFieldValues.Scoped} values.
     */
    public static final class MemoryEntry extends PreHashed {
        private final ScopedValueId[] scopedValueIds;

        public MemoryEntry(ScopedValueId[] scopedValueIds) {
            super(hashOf(scopedValueIds));
            this.scopedValueIds = scopedValueIds;
        }

        private MemoryEntry(ScopedValueId single) {
            this(new ScopedValueId[]{single});
        }

        private static int hashOf(ScopedValueId[] scopedValueIds) {
            int hash = 0;
            for (ScopedValueId single : scopedValueIds) {
                hash += hash * 31 + single.hashCode();
            }
            return hash;
        }

        ScopedValueId[] scopedValues() {
            return scopedValueIds;
        }

        MemoryEntry toMainScope() {
            ScopedValueId[] newKeys = new ScopedValueId[scopedValueIds.length];
            for (int i = 0; i < scopedValueIds.length; i++) {
                newKeys[i] = scopedValueIds[i].toScope(MemoryScope.MAIN);
            }
            return new MemoryEntry(newKeys);
        }

        static MemoryEntry fromEntryNode(long values, MemoryScope scope) {
            return new MemoryEntry(new ScopedValueId(values, scope));
        }

        public ScopedValueId[] getScopedValueIds() {
            return scopedValueIds;
        }

//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//            MemoryEntry entry = (MemoryEntry) o;
//            return Arrays.equals(scopedValueIds, entry.scopedValueIds);
//        }
//
//        @Override
//        public String toString() {
//            return Arrays.toString(scopedValueIds);
//        }
    }

    /**
     * A simple and memory efficient wrapper for field values identifiers (see {@link org.evrete.api.spi.ValueIndexer})
     */
    public static class ScopedValueId {
        private final long valueId;
        private final byte scope;

        public ScopedValueId(long valueId, MemoryScope scope) {
            this(valueId, toByte(scope));
        }

        private ScopedValueId(long valueId, byte scope) {
            this.valueId = valueId;
            this.scope = scope;
        }

        public MemoryScope getScope() {
            return fromByte(scope);
        }

        public ScopedValueId toScope(MemoryScope scope) {
            byte b;
            if (this.scope == (b = toByte(scope))) {
                return this;
            } else {
                return new ScopedValueId(valueId, b);
            }
        }

        public long getValueId() {
            return valueId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScopedValueId that = (ScopedValueId) o;
            return valueId == that.valueId && scope == that.scope;
        }

        @Override
        public int hashCode() {
            return (int) valueId;
        }

        static byte toByte(MemoryScope scope) {
            return (byte) (scope == MemoryScope.MAIN ? 0 : 1);
        }

        static MemoryScope fromByte(byte b) {
            return b == 0 ? MemoryScope.MAIN : MemoryScope.DELTA;
        }
    }

    public static class DeletePredicate implements Predicate<ConditionMemory.MemoryEntry> {
        private final int index;
        private final Set<Long> valuesToDelete;

        DeletePredicate(int index, Set<Long> valuesToDelete) {
            this.index = index;
            this.valuesToDelete = valuesToDelete;
        }

        @Override
        public boolean test(ConditionMemory.MemoryEntry memoryEntry) {
            ConditionMemory.ScopedValueId v = memoryEntry.getScopedValueIds()[index];
            return valuesToDelete.contains(v.getValueId());
        }

        @NonNull
        public static Predicate<ConditionMemory.MemoryEntry> ofMultipleOR(@NonNull Collection<DeletePredicate> predicates) {
            Iterator<DeletePredicate> iterator = predicates.iterator();
            if (iterator.hasNext()) {
                Predicate<ConditionMemory.MemoryEntry> predicate = iterator.next();
                while (iterator.hasNext()) {
                    predicate = predicate.or(iterator.next());
                }
                return predicate;
            } else {
                return memoryEntry -> false;
            }
        }
    }

}
