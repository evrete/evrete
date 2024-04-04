package org.evrete.api;

import org.evrete.api.annotations.Unstable;

import java.util.Collection;
import java.util.zip.ZipEntry;

/**
 * This interface represents the engine's alpha and beta memories as referenced in the RETE algorithm.
 * Unlike the traditional approach, this engine stores fact handles in these memories instead of the facts themselves.
 * The mapping between fact handles and their corresponding facts is managed by another memory structure, {@link FactStorage}.
 * <p>
 * Effectively, the {@code KeyedFactStorage} can be represented as a {@code Map<Object[], Collection<FactHandle>>}, where
 * the keys are field values from facts of the same or different types.
 * </p>
 * <p>
 * The engine maintains as many instances of {@code KeyedFactStorage} as there are unique combinations of fields
 * in the rules' conditions. For example, a condition like <code>$customer.id == $invoice.billTo</code> will result
 * in a new {@code KeyedFactStorage} being created, with the keys being a sequence of the customer's ID and the invoice's bill-to fields.
 * </p>
 * <p>
 * As far as write operations are concerned, this storage behaves similarly to {@link java.util.jar.JarOutputStream}: first, we write the memory's key, and then the fact handles themselves.
 * </p>
 */
@Unstable
public interface KeyedFactStorage {
    /**
     * Retrieves an iterator over the keys in the memory with the specified {@link KeyMode}.
     *
     * @param keyMode the mode in which the keys are retrieved
     * @return an iterator over the keys in the memory
     */
    ReIterator<MemoryKey> keys(KeyMode keyMode);

    /**
     * Returns an iterator over the values in the {@code KeyedFactStorage} with the specified {@link KeyMode}
     * and {@link MemoryKey}.
     *
     * @param mode the mode in which the values are retrieved
     * @param key the memory key
     * @return an iterator over the values
     */
    ReIterator<FactHandleVersioned> values(KeyMode mode, MemoryKey key);

    /**
     * Commits the changes made in the KeyedFactStorage.
     */
    void commitChanges();

    /**
     * Clears the data stored in the memory.
     */
    void clear();


    /**
     * <p>
     * This method is similar to the {@link java.util.jar.JarOutputStream#putNextEntry(ZipEntry)}. However,
     * it expects both sides to know the number of keys to be written until the {@link #write(Collection)} method
     * is called.
     * </p>
     *
     * @param partialKey The next component of the memory key.
     */
    void write(FieldValue partialKey);

    /**
     * <p>
     * This method will be called after the necessary number of keys are provided via {@link #write(FieldValue)}.
     * After the fact handles are provided, the implementation must reset its internal key counter and wait for the
     * next call to {@link #write(FieldValue)}.
     * </p>
     *
     * @param factHandles fact handles to save under the sequence of keys
     */
    void write(Collection<FactHandleVersioned> factHandles);
}
