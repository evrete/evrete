package org.evrete.api;

import org.evrete.api.spi.DeltaInsertMemory;
import org.evrete.api.spi.MemoryScope;

import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Describes a general session-level memory interface related to the Rete algorithm.
 * This implementation distinguishes three general Rete stages:
 * <ol>
 *   <li>
 *       Computing delta objects, i.e., changes in working memory structures caused by various actions
 *       like memory inserts.
 *   </li>
 *   <li>
 *       Acting on these changes, for example computing condition memories or executing a rule's RHS.
 *   </li>
 *   <li>
 *       Committing the changes, like saving the changes into permanent storage.
 *   </li>
 * </ol>
 *
 * @param <M> the type of elements stored inside the memory
 *
 */
public interface ReteMemory<M> extends DeltaInsertMemory {

    /**
     * Provides an iterator over permanent or temporary (uncommitted) storage depending on the specified scope.
     *
     * @param scope the memory scope indicating whether to iterate over main or delta storage
     * @return an iterator over storage elements of type M depending on the specified scope
     */
    Iterator<M> iterator(MemoryScope scope);

    /**
     * Provides a default implementation for streaming over stored elements based on the provided scope.
     * Implementations are encouraged to override this method to create parallel streams, if possible.
     *
     * @param scope the memory scope indicating whether to stream over main or delta storage
     * @return a stream over elements in the storage of the provided scope
     */
    default Stream<M> stream(MemoryScope scope) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator(scope), 0),
                false
        );
    }

}


