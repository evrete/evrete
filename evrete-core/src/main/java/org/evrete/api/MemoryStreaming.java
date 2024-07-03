package org.evrete.api;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Provides streaming methods for handling in-memory facts.
 * This interface is extended by both {@link StatefulSession} and {@link StatelessSession}.
 * Note that for stateless sessions, invoking any streaming method will trigger the session to fire,
 * and no further operations on the session will be possible afterward.
 */
public interface MemoryStreaming {

    /**
     * Streams all fact entries as a {@link Stream} of {@link Map.Entry} objects.
     *
     * @return a stream of all fact entries and their fact handles
     */
    Stream<Map.Entry<FactHandle, Object>> streamFactEntries();

    /**
     * Streams all fact entries of a specific type as a {@link Stream} of {@link Map.Entry} objects.
     *
     * @param <T>  the type of facts to stream
     * @param type the logical type of the facts
     * @return a stream of fact entries of the specified logical type
     * @see Type about logical types
     */
    <T> Stream<Map.Entry<FactHandle, T>> streamFactEntries(String type);

    /**
     * Streams all fact entries of a specific Java type as a {@link Stream} of {@link Map.Entry} objects.
     *
     * @param <T>  the type of facts to stream
     * @param type the Java type of the facts
     * @return a stream of fact entries of the specified logical type
     */
    <T> Stream<Map.Entry<FactHandle, T>> streamFactEntries(Class<T> type);

    /**
     * Streams all facts as a {@link Stream} of {@link Object} values.
     *
     * @return a stream of all facts
     */
    default Stream<Object> streamFacts() {
        return streamFactEntries().map(Map.Entry::getValue);
    }

    /**
     * Streams all facts of a specific type as a {@link Stream} of {@link Object} values.
     *
     * @param <T>  the type of facts to stream
     * @param type the logical type of the facts as a string
     * @return a stream of facts of the specified logical type
     * @see Type about logical types
     */
    default <T> Stream<T> streamFacts(String type) {
        return this.<T>streamFactEntries(type).map(Map.Entry::getValue);
    }

    /**
     * Streams all facts of a specific type as a {@link Stream} of {@link Object} values.
     *
     * @param <T>  the type of facts to stream
     * @param type the Java type of the facts
     * @return a stream of facts of the specified type
     */
    default <T> Stream<T> streamFacts(Class<T> type) {
        return this.streamFactEntries(type).map(Map.Entry::getValue);
    }
}
