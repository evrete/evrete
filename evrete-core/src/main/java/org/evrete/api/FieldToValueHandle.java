package org.evrete.api;

import org.evrete.runtime.ActiveField;

import java.util.function.Function;

@FunctionalInterface
public interface FieldToValueHandle extends Function<ActiveField, ValueHandle> {
}
