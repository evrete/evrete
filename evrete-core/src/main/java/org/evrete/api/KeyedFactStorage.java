package org.evrete.api;

public interface KeyedFactStorage extends InnerFactMemory {
    ReIterator<MemoryKey> keys(KeyMode keyMode);

    ReIterator<FactHandleVersioned> values(KeyMode mode, MemoryKey key);

    /**
     * <p>
     * Method stores fact handle under specific field values key. A precomputed
     * hash code of the key is also provided, however implementations may disregard it.
     * </p>
     *
     * @param key     field values key
     * @param keyHash precomputed key hash code
     * @param value   fact handle to save
     */
    void insert(FieldToValueHandle key, int keyHash, FactHandleVersioned value);

    /**
     * <p>
     * Returns <code>0</code> if storage is absolutely empty, <code>2</code> if only main
     * storage has data, <code>1</code> if only delta data is available, and <code>3</code>
     * if both delta and main data containers are not empty. The return value will be used
     * to determine which condition graphs to process, and/or which rules to activate.
     * </p>
     * <p>
     * Performance-wise, it is recommended to pre-compute the return value during inserts
     * and reset it to zero on the {@link #commitChanges()} call.
     * </p>
     *
     * @return int mask as per description
     */
    default int getDeltaStatus() {
        int mask = 0;
        for (KeyMode mode : KeyMode.values()) {
            if (keys(mode).reset() > 0) {
                mask |= mode.getDeltaMask();
            }
        }
        return mask;
    }
}
