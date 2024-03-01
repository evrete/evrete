package org.evrete.api;

import java.util.Collection;
import java.util.zip.ZipEntry;

public interface KeyedFactStorage extends InnerFactMemory {
    ReIterator<MemoryKey> keys(KeyMode keyMode);

    ReIterator<FactHandleVersioned> values(KeyMode mode, MemoryKey key);

    /**
     * <p>
     * This method is similar to the {@link java.util.jar.JarOutputStream#putNextEntry(ZipEntry)}. However,
     * it expects both sides to know the number of keys to be written until the {@link #write(Collection)} method
     * is called.
     * </p>
     *
     * @param partialKey The next component of the memory key.
     */
    void write(ValueHandle partialKey);

    /**
     * <p>
     * This method will be called after the necessary number of keys are provided via {@link #write(ValueHandle)}.
     * After the fact handles are provided, the implementation must reset its internal key counter and wait for the
     * next call to {@link #write(ValueHandle)}.
     * </p>
     *
     * @param factHandles fact handles to save under the sequence of keys
     */
    void write(Collection<FactHandleVersioned> factHandles);
}
