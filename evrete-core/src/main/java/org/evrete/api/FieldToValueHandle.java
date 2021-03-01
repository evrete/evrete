package org.evrete.api;

import java.util.function.Function;

@FunctionalInterface
public interface FieldToValueHandle extends Function<ActiveField, ValueHandle> {
}
