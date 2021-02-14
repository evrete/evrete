package org.evrete.api;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

//TODO javadoc
public interface StatefulSession extends KnowledgeSession<StatefulSession> {

    StatefulSession setFireCriteria(BooleanSupplier fireCriteria);

    void close();


    RuntimeRule getRule(String name);

    List<RuntimeRule> getRules();

    default RuntimeRule getRule(Named named) {
        return getRule(named.getName());
    }

    default void insertAndFire(Collection<?> objects) {
        insert(objects);
        fire();
    }

    default void insertAndFire(Object... objects) {
        insert(objects);
        fire();
    }

    void clear();

    void forEachFact(BiConsumer<FactHandle, Object> consumer);

    @Deprecated
    default void deleteAndFire(Object... objects) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    default void deleteAndFire(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    default void updateAndFire(Object... objects) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    default void updateAndFire(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    default void update(Collection<?> objects) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Deprecated
    default void update(Object fact) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Deprecated
    default void delete(Object fact) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Deprecated
    default <T> void forEachMemoryObject(String type, Consumer<T> consumer) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Deprecated
    default void delete(Object... objects) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    default void update(Object... objects) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    default <T> void forEachMemoryObject(Class<T> type, Consumer<T> consumer) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Deprecated
    default void forEachMemoryObject(Consumer<Object> consumer) {
        throw new UnsupportedOperationException("Deprecated");
    }

}
