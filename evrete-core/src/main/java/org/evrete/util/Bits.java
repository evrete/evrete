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

    public static <T extends Masked> Bits or(Collection<T> collection) {
        return or(collection, Masked::getMask);
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

/*
    public static <T> Bits of(T[] collection, ToIntFunction<T> mapping) {
        Bits b = new Bits();
        for (T o : collection) {
            b.set(mapping.applyAsInt(o));
        }
        return b;
    }
*/

/*
    public static <T> Bits of(T o, ToIntFunction<T> mapping) {
        Bits b = new Bits();
        b.set(mapping.applyAsInt(o));
        return b;
    }
*/

    public void or(Bits other) {
        delegate.or(other.delegate);
    }


    public void set(int index) {
        delegate.set(index);
    }

    public boolean intersects(Bits other) {
        return delegate.intersects(other.delegate);
    }

/*
    public boolean containsAll(Bits other) {
        PrimitiveIterator.OfInt i = other.delegate.stream().iterator();
        while (i.hasNext()) {
            int setIndex = i.nextInt();
            if (!this.delegate.get(setIndex)) {
                return false;
            }
        }
        return true;
    }
*/

/*
    public IntStream stream() {
        return delegate.stream();
    }
*/

/*
    public int[] getBitsAsArray() {
        int[] arr = new int[delegate.cardinality()];
        AtomicInteger i = new AtomicInteger(0);
        stream().forEach(value -> arr[i.getAndIncrement()] = value);
        return arr;
    }
*/

    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bits bits = (Bits) o;
        return delegate.equals(bits.delegate);
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
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
