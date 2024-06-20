package org.evrete.api;

import java.util.stream.Stream;

//TODO move to SessionOps or (vice versa)
public interface MemoryStreaming {
    //TODO !!!! important: use a provided delayed executor to check memories, especially session memory scans and retrievals
    Stream<MapEntry<FactHandle, Object>> streamFactEntries();

    //TODO !!!! important: use a provided delayed executor to check memories, especially session memory scans and retrievals
    <T> Stream<MapEntry<FactHandle, T>> streamFactEntries(String type);

    //TODO !!!! important: use a provided delayed executor to check memories, especially session memory scans and retrievals
    <T> Stream<MapEntry<FactHandle, T>> streamFactEntries(Class<T> type);

    default Stream<Object> streamFacts() {
        return streamFactEntries().map(MapEntry::getValue);
    }

    default <T> Stream<T> streamFacts(String type) {
        return this.<T>streamFactEntries(type).map(MapEntry::getValue);
    }

    default <T> Stream<T> streamFacts(Class<T> type) {
        return this.streamFactEntries(type).map(MapEntry::getValue);
    }
}
