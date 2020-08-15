package org.evrete.api;

public interface RhsContext {
    default Object getObject(String name) {
        return getFact(name).getDelegate();
    }

    RuntimeFact getFact(String name);

    //TODO !!! check if fact fields have _really_ changed
    RhsContext update(Object obj);

    RhsContext delete(Object obj);

    //TODO add tests
    default RhsContext deleteFact(String factRef) {
        return delete(getObject(factRef));
    }

    //TODO add tests
    default RhsContext updateFact(String factRef) {
        return update(getObject(factRef));
    }

    RhsContext insert(Object obj);

    @SuppressWarnings("unchecked")
    default <T> T get(String name) {
        return (T) getObject(name);
    }

}
