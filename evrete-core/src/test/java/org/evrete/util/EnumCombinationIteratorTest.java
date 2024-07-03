package org.evrete.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class EnumCombinationIteratorTest {

    @Test
    void test1() {
        for (int size = 0; size < 8; size++) {
            E1[] result = new E1[size];
            EnumCombinationIterator<E1> it = new EnumCombinationIterator<>(E1.class, result);

            int counter = 0;
            Set<List<E1>> sets = new HashSet<>();
            while (it.hasNext()) {
                E1[] val = it.next();
                // Identity check
                assert val == result;
                List<E1> set = Arrays.asList(val);
                assert !sets.contains(set);
                sets.add(set);
                counter++;
            }
            assert counter == Math.pow(E1.values().length, result.length) ;
        }
    }

    @Test
    void test2() {
        for (int size = 0; size < 8; size++) {
            E2[] result = new E2[size];
            EnumCombinationIterator<E2> it = new EnumCombinationIterator<>(E2.class, result);

            int counter = 0;
            Set<List<E2>> sets = new HashSet<>();
            while (it.hasNext()) {
                E2[] val = it.next();
                // Identity check
                assert val == result;
                List<E2> set = Arrays.asList(val);
                assert !sets.contains(set);
                sets.add(set);
                counter++;
            }
            assert counter == Math.pow(E2.values().length, result.length) ;
        }
    }

    @Test
    void test3() {
        for (int size = 0; size < 8; size++) {
            E3[] result = new E3[size];
            EnumCombinationIterator<E3> it = new EnumCombinationIterator<>(E3.class, result);

            int counter = 0;
            Set<List<E3>> sets = new HashSet<>();
            while (it.hasNext()) {
                E3[] val = it.next();
                // Identity check
                assert val == result;
                List<E3> set = Arrays.asList(val);
                assert !sets.contains(set);
                sets.add(set);
                counter++;
            }
            assert counter == Math.pow(E3.values().length, result.length) ;
        }
    }


    enum E1 {
        A
    }
    enum E2 {
        A,B
    }
    enum E3 {
        A,B,C
    }
}
