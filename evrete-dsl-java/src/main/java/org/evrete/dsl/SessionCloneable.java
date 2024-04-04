package org.evrete.dsl;

interface SessionCloneable<T> {
    T copy(Object sessionInstance);
}
