package org.evrete.api;

import org.evrete.api.annotations.Unstable;

/**
 * This interface defines methods for resolving field values and retrieving them.
 */
@Unstable
public interface ValueResolver {
    FieldValue getValueHandle(Class<?> valueType, Object value);

    Object getValue(FieldValue handle);

}
