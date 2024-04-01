package org.evrete.spi.minimal;

import org.evrete.api.IntToValue;
import org.evrete.api.FieldValue;

import java.util.function.IntFunction;

/**
 * <p>
 * See {@link IntToValue} for description
 * </p>
 */
@FunctionalInterface
interface IntToValueHandle extends IntFunction<FieldValue> {
}
