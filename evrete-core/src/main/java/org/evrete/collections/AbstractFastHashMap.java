package org.evrete.collections;

import org.evrete.api.MapEntry;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractFastHashMap<K, V> extends AbstractFastHashMapBase<K, AbstractFastHashMap.Entry<K, V>> {
    private static final int DEFAULT_INITIAL_SIZE = 16;

    public AbstractFastHashMap() {
        this(DEFAULT_INITIAL_SIZE);
    }

    public AbstractFastHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        int addr = getStorePosition(key, true);
        Entry<K, V> prev = (Entry<K, V>) data[addr];
        if (prev == null) {
            data[addr] = new Entry<>(key, value);
            addNew(addr);
            size++;
            return null;
        } else {
            V v = prev.value;
            prev.value = value;
            if (deletedIndices[addr]) {
                deletedIndices[addr] = false;
                size++;
                deletes--;
            }
            return v;
        }
    }

    public V remove(K key) {
        Entry<K, V> e = super.removeKey(key);
        return e == null ? null : e.value;
    }


    public V get(K key) {
        Entry<K, V> found = getEntry(key);
        return found == null ? null : found.value;
    }

    public final void forEachEntry(BiConsumer<K, V> consumer) {
        forEachDataEntry(e -> consumer.accept(e.key, e.value));
    }

    public final void forEachValue(Consumer<V> consumer) {
        forEachDataEntry(e -> consumer.accept(e.value));
    }

    public void forEachKey(Consumer<K> consumer) {
        forEachDataEntry(e -> consumer.accept(e.key));
    }

    public final V computeIfAbsent(K key, Function<K, V> function) {
        return super.computeEntryIfAbsent(key, k -> new Entry<>(k, function.apply(k))).value;
    }

    public static class Entry<K1, V1> extends HashEntry<K1> implements MapEntry<K1, V1> {
        V1 value;

        public Entry(K1 key, V1 value) {
            super(key);
            this.value = value;
        }

        @Override
        public V1 getValue() {
            return value;
        }

        @Override
        public K1 getKey() {
            return key;
        }

        @Override
        public String toString() {
            return "{" + key +
                    "=" + value +
                    '}';
        }
    }
}