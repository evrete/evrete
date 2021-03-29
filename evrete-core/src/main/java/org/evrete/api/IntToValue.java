package org.evrete.api;

import java.util.function.IntFunction;

/**
 * <p>
 * A convenient "random object accessor", i.e. function that converts an integer address
 * to an Object. For example, for an array of Objects <code>Object[] arr</code> IntToValue
 * can be expressed as <code>IntToValue itv = i -&gt; arr[i]</code>
 * </p>
 */
@FunctionalInterface
public interface IntToValue extends IntFunction<Object> {
}
