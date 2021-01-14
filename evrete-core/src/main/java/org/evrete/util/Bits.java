package org.evrete.util;

import org.evrete.api.Copyable;
import org.evrete.api.Masked;

import java.util.BitSet;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Bits implements Copyable<Bits>, Masked {
    private final BitSet delegate;

    private Bits(BitSet delegate) {
        this.delegate = delegate;
    }

    public Bits() {
        this(new BitSet());
    }

    public static <T> Set<T> matchesOR(Bits mask, Collection<T> collection, Function<T, Bits> maskFunction) {
        return collection.stream()
                .filter(node -> maskFunction.apply(node).intersects(mask))
                .collect(Collectors.toSet());

    }

    private static <T> Bits or(Collection<T> collection, Function<T, Bits> mapping) {
        Bits b = new Bits();
        for (T o : collection) {
            b.or(mapping.apply(o));
        }
        return b;
    }

    @Override
    public Bits getMask() {
        return this;
    }

    public void or(Bits other) {
        delegate.or(other.delegate);
    }


    public void set(int index) {
        delegate.set(index);
    }

    public boolean intersects(Bits other) {
        return delegate.intersects(other.delegate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bits bits = (Bits) o;
        return delegate.equals(bits.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public Bits copyOf() {
        return new Bits((BitSet) delegate.clone());
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
