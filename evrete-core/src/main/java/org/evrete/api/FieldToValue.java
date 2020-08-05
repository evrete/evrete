package org.evrete.api;

import java.util.function.Function;

@FunctionalInterface
public interface FieldToValue extends Function<ActiveField, Object> {
}
