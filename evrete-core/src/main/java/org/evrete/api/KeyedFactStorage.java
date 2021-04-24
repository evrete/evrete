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
}
