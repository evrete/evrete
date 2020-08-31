package org.evrete.util;

import org.evrete.api.ThreadUnsafe;
import org.evrete.api.ValueRow;

import java.util.function.Function;
import java.util.function.Supplier;

public class ValueRowToArray implements Function<ValueRow, ValueRow[]> {
    public static final Supplier<Function<ValueRow, ValueRow[]>> SUPPLIER = ValueRowToArray::new;
    private final ValueRow[] shared = new ValueRow[1];

    @Override
    @ThreadUnsafe
    public ValueRow[] apply(ValueRow valueRow) {
        this.shared[0] = valueRow;
        return this.shared;
    }
}
