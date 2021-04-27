package org.evrete.util;

import org.evrete.runtime.FactType;
import org.evrete.runtime.evaluation.MemoryAddress;

import java.util.BitSet;
import java.util.function.ToIntFunction;

public class Mask<T> {
    public static BitSet EMPTY = new BitSet();
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

    public void clear() {
        delegate.clear();
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
}
