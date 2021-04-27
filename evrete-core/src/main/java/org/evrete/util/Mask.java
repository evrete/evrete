package org.evrete.util;

import org.evrete.api.Type;
import org.evrete.runtime.evaluation.MemoryAddress;

import java.util.function.ToIntFunction;

public class Mask<T> extends Bits {
    private final ToIntFunction<T> intMapper;

    private Mask(ToIntFunction<T> intMapper) {
        this.intMapper = intMapper;
    }

    public static Mask<Type<?>> typeMask() {
        return new Mask<>(Type::getId);
    }

    public static Mask<MemoryAddress> addressMask() {
        return new Mask<>(MemoryAddress::getBucketIndex);
    }

    public void set(T obj) {
        super.set(intMapper.applyAsInt(obj));
    }

    public boolean get(T obj) {
        return super.get(intMapper.applyAsInt(obj));
    }


}
