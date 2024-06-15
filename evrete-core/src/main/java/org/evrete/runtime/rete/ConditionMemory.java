package org.evrete.runtime.rete;

import org.evrete.api.ReteMemory;
import org.evrete.api.spi.MemoryScope;
import org.evrete.runtime.FactFieldValues;
import org.evrete.runtime.GroupedFactType;

import java.util.Arrays;
import java.util.Iterator;
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
            throw new IllegalArgumentException("Unknown destination: " + destination);
        }
    }

    @Override
    public void commit() {
        Iterator<MemoryEntry> iterator = delta.iterator();
        while (iterator.hasNext()) {
            MemoryEntry entry = iterator.next();
            main.add(entry.toMainScope(MemoryScope.MAIN));
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
     * Array indices correspond to the values defined by the
     * {@link GroupedFactType#getInGroupIndex()} method.
     * <p>
     * Array values are allowed to be null in missing positions but the terminal node
     * should have all the components be non-null.
     * </p>
     */
    public static final class MemoryEntry {
        private final FactFieldValues.Scoped[] scopedKeys;

        private MemoryEntry(FactFieldValues.Scoped[] scopedKeys) {
            this.scopedKeys = scopedKeys;
        }

        FactFieldValues.Scoped[] scopedValues() {
            return scopedKeys;
        }

        // TODO create tests and see how these scopes behave in a hashed collection
        MemoryEntry toMainScope(MemoryScope scope) {
            FactFieldValues.Scoped[] newKeys = new FactFieldValues.Scoped[scopedKeys.length];
            for (int i = 0; i < scopedKeys.length; i++) {
                FactFieldValues.Scoped currentKey = scopedKeys[i];
                newKeys[i] = currentKey == null ? null : currentKey.toScope(scope);
            }
            return new MemoryEntry(newKeys);
        }

        static MemoryEntry fromEntryNode(FactFieldValues values, MemoryScope scope, int totalFactTypes, int inGroupIndex) {
            FactFieldValues.Scoped[] array = new FactFieldValues.Scoped[totalFactTypes];
            array[inGroupIndex] = new FactFieldValues.Scoped(values, scope);
            return new MemoryEntry(array);
        }

        public static MemoryEntry fromDeltaState(int totalFactTypes, MemoryEntry[] state, ReteSessionNode[] sources) {
            FactFieldValues.Scoped[] array = new FactFieldValues.Scoped[totalFactTypes];
            ReteSessionNode source;
            for (int i = 0; i < sources.length; i++) {
                source = sources[i];
                MemoryEntry entry = state[i];
                for (int groupIndex : source.inGroupIndices) {
                    assert array[groupIndex] == null;
                    array[groupIndex] = entry.scopedKeys[groupIndex];
                }
            }
            return new MemoryEntry(array);
        }

        public FactFieldValues.Scoped get(int index) {
            return scopedKeys[index];
        }


        @Override
        public String toString() {
            return Arrays.toString(scopedKeys);
        }
    }
}
