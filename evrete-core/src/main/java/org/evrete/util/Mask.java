package org.evrete.util;

import org.evrete.api.Type;
import org.evrete.runtime.evaluation.MemoryAddress;

import java.util.BitSet;
import java.util.function.ToIntFunction;

public class Mask<T> {
    private final BitSet delegate = new BitSet();
    private final ToIntFunction<T> intMapper;

    private Mask(ToIntFunction<T> intMapper) {
        this.intMapper = intMapper;
    }

    public static Mask<Type<?>> typeMask() {
        return new Mask<>(Type::getId);
    }

    public static Mask<MemoryAddress> addressMask() {
        return new Mask<>(MemoryAddress::getId);
    }

    public void set(T obj) {
        delegate.set(intMapper.applyAsInt(obj));
    }

    public boolean get(T obj) {
        return delegate.get(intMapper.applyAsInt(obj));
    }

    public int cardinality() {
        return delegate.cardinality();
    }
}
