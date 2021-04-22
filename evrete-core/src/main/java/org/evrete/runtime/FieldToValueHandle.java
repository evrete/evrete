package org.evrete.runtime;

import org.evrete.api.ValueHandle;

import java.util.function.Function;

@FunctionalInterface
interface FieldToValueHandle extends Function<ActiveField, ValueHandle> {
}
