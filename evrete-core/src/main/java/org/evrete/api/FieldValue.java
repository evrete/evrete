package org.evrete.api;

/**
 * A marker interface that uniquely identifies the value of a field. The engine uses this abstraction
 * in RETE's condition evaluation instead of actual field values. Implementations are expected to
 * override or delegate <code>hashCode()</code> and <code>equals(Object obj)</code> methods
 * to the actual field value.
 */
public interface FieldValue {
}
