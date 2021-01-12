package org.evrete.api;

import org.evrete.runtime.ActiveField;

import java.util.function.Function;

@FunctionalInterface
public interface FieldToValue extends Function<ActiveField, Object> {
}
