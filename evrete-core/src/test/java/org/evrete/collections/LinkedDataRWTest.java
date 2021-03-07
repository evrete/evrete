package org.evrete.collections;

import org.evrete.api.ReIterator;
import org.junit.jupiter.api.Test;

import java.util.*;

class LinkedDataRWTest {

    @Test
    void test1() {
        LinkedDataRW<String> ld = new LinkedDataRW<>();
        ld.add("a").add("b").add("c");

        ReIterator<String> it = ld.iterator();
        Set<String> set = new HashSet<>();
        while (it.hasNext()) {
            set.add(it.next());
        }
        assert set.size() == 3;
        assert set.size() == it.reset();
    }

    @Test
    void test2() {
        LinkedDataRW<String> ld = new LinkedDataRW<>();
        ld.add("a").add("b").add("c").add("d");

        ReIterator<String> it = ld.iterator();
        Set<String> set = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            while (it.hasNext()) {
                set.add(it.next());
            }
            long size = it.reset();
            assert set.size() == size;
            assert set.size() == size;
            set.clear();
        }
    }

    @Test
    void test3() {
        LinkedDataRW<String> ld = new LinkedDataRW<>();
        ld.add("a").add("b").add("c").add("d");


        List<String> list = new LinkedList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");
        ReIterator<String> it1 = ld.iterator();

        for (int cnt = 0; cnt < 100; cnt++) {
            Iterator<String> it2 = list.iterator();
            for (int i = 0; i < 4; i++) {
                String s1 = it1.next();
                String s2 = it2.next();
                assert s1.equals(s2);
            }
            it1.reset();
        }
    }
}