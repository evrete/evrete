package org.evrete.runtime;

import org.evrete.runtime.evaluation.AlphaConditionHandle;
import org.evrete.util.IndexedValue;

import java.util.BitSet;
import java.util.Collection;
import java.util.Objects;
import java.util.function.ToIntFunction;

public final class Mask<T> {
    public static final BitSet EMPTY = new BitSet();
    private final BitSet delegate = new BitSet();
    private final ToIntFunction<T> intMapper;

    private Mask(ToIntFunction<T> intMapper) {
        this.intMapper = intMapper;
    }

    BitSet getDelegate() {
        return delegate;
    }

    public void or(Mask<T> other) {
        delegate.or(other.delegate);
    }


    public boolean containsAll(Mask<T> value) {
        BitSet other = value.delegate;
        if(other.isEmpty() && delegate.isEmpty()) {
            return true;
        } else {
            BitSet otherCloned = (BitSet) other.clone();
            otherCloned.and(delegate);
            return otherCloned.equals(other);
        }
    }

    public Mask<T> set(T obj) {
        delegate.set(intMapper.applyAsInt(obj));
        return this;
    }

    public Mask<T> set(T obj, boolean flag) {
        delegate.set(intMapper.applyAsInt(obj), flag);
        return this;
    }

    public int[] getSetBits() {
        return delegate.stream().toArray();
    }

    public Mask<T> set(T[] arr) {
        for (T obj : arr) {
            this.set(obj);
        }
        return this;
    }

    public Mask<T> set(Collection<? extends T> arr) {
        for (T obj : arr) {
            this.set(obj);
        }
        return this;
    }

    public boolean get(T obj) {
        return delegate.get(intMapper.applyAsInt(obj));
    }

    public boolean getRaw(int index) {
        return delegate.get(index);
    }

    public int cardinality() {
        return delegate.cardinality();
    }

    public int length() {
        return delegate.length();
    }

    public boolean intersects(Mask<T> other) {
        return delegate.intersects(other.delegate);
    }


//    public static Mask<ZZZMemoryAddress> addressMask() {
//        return new Mask<>(ZZZMemoryAddress::getId);
//    }

    public static <I extends IndexedValue<?>> Mask<I> ofIndexed() {
        return new Mask<>(value -> value.getIndex());
    }

    public static <I> Mask<I> instance(ToIntFunction<I> intMapper) {
        return new Mask<>(intMapper);
    }


    public static <I extends IndexedValue<?>> Mask<I> or(Collection<Mask<I>> collection) {
        Mask<I> result = ofIndexed();
        for (Mask<I> mask : collection) {
            result.or(mask);
        }
        return result;
    }

    public static Mask<FactType> factTypeMask() {
        return new Mask<>(FactType::getInRuleIndex);
    }

    public static Mask<AlphaConditionHandle> alphaConditionsMask() {
        return Mask.instance(AlphaConditionHandle::getIndex);
    }

    public static Mask<ActiveType> typeMask() {
        return new Mask<>(value -> value.getId().getIndex());
    }

    public static Mask<GroupedFactType> inGroupMask() {
        return new Mask<>(GroupedFactType::getInGroupIndex);
    }

    public static Mask<AlphaAddress> alphaAddressMask() {
        return Mask.instance(AlphaAddress::getIndex);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mask<?> mask = (Mask<?>) o;
        return Objects.equals(delegate, mask.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
