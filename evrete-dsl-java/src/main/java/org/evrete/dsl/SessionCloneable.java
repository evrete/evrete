package org.evrete.dsl;

public interface SessionCloneable<T> {
    T copy(Object sessionInstance);
}
