package org.evrete.runtime;

import org.junit.jupiter.api.Test;

import java.util.BitSet;

class MaskTest {

    @Test
    void containsAll() {
        Mask<Integer> mask1 = Mask.instance(i->i);

        mask1.set(2).set(3).set(4);
        BitSet set1 = (BitSet) mask1.getDelegate().clone();

        Mask<Integer> mask2 = Mask.instance(i->i);
        mask2.set(2).set(3);
        BitSet set2 = (BitSet) mask2.getDelegate().clone();

        // Main test
        assert mask1.containsAll(mask2);
        // Assert that nothing's been mutated
        assert set1.equals(mask1.getDelegate());
        assert set2.equals(mask2.getDelegate());


        // Inverse test
        assert !mask2.containsAll(mask1);
        // Assert that nothing's been mutated
        assert set1.equals(mask1.getDelegate());
        assert set2.equals(mask2.getDelegate());

        // Self tests
        assert mask1.containsAll(mask1);
        assert mask2.containsAll(mask2);
        // Assert that nothing's been mutated
        assert set1.equals(mask1.getDelegate());
        assert set2.equals(mask2.getDelegate());




    }
}
