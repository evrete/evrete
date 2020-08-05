package org.evrete.api;

import org.evrete.util.Bits;

public interface Masked {
    Bits getMask();

    default boolean testOR(Masked other) {
        Bits b1 = getMask();
        Bits b2 = other.getMask();
        return b1.intersects(b2);
    }
}
