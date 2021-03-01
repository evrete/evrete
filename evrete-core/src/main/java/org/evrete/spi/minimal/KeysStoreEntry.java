package org.evrete.spi.minimal;

import org.evrete.api.KeysStore;
import org.evrete.api.MemoryKey;

import java.util.Arrays;

abstract class KeysStoreEntry implements KeysStore.Entry {
    final MemoryKey[] key;
    final int hash;

    KeysStoreEntry(MemoryKey[] key, int hash) {
        this.key = key;
        this.hash = hash;
    }

    @Override
    final public MemoryKey[] key() {
        return key;
    }

    final boolean eq(KeysStoreEntry other) {
        return Arrays.equals(key, other.key);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return eq((KeysStoreEntry) o);
    }

    @Override
    public final int hashCode() {
        return hash;
    }

}
