package org.evrete.runtime;

import java.util.BitSet;
import java.util.function.ToIntFunction;

public final class Mask<T> {
    public static final BitSet EMPTY = new BitSet();
    private final BitSet delegate = new BitSet();
    private final ToIntFunction<T> intMapper;

    private Mask(ToIntFunction<T> intMapper) {
        this.intMapper = intMapper;
    }

    public static Mask<MemoryAddress> addressMask() {
        return new Mask<>(MemoryAddress::getId);
    }

    public static Mask<FactType> factTypeMask() {
        return new Mask<>(FactType::getInRuleIndex);
    }

    public void or(Mask<T> other) {
        delegate.or(other.delegate);
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

    public boolean intersects(Mask<T> other) {
        return delegate.intersects(other.delegate);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
