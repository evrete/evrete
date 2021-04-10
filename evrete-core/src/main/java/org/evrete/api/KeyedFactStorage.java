package org.evrete.api;

import java.util.Collection;
import java.util.zip.ZipEntry;

public interface KeyedFactStorage extends InnerFactMemory {
    ReIterator<MemoryKey> keys(KeyMode keyMode);

    ReIterator<FactHandleVersioned> values(KeyMode mode, MemoryKey key);

    /**
     * <p>
     * Method similar to the {@link java.util.jar.JarOutputStream#putNextEntry(ZipEntry)}, except
     * both sides are expected to know how many keys are to be written until  {@link #write(Collection)}
     * gets called.
     * </p>
     *
     * @param partialKey next component of the memory key
     */
    void write(ValueHandle partialKey);

    /**
     * <p>
     * This method will be called after necessary count of keys are provided via {@link #write(ValueHandle)}.
     * After fact handles are provided, the implementation must reset its internal key counter and wait for the
     * next call of {@link #write(ValueHandle)}.
     * </p>
     *
     * @param factHandles fact handles to save under the sequence of keys
     */
    void write(Collection<FactHandleVersioned> factHandles);

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
