package org.evrete.collections;

import org.evrete.api.ReIterator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

class LinkedDataRWDTest {

    private static <T> boolean sameData(LinkedDataRWD<T> data1, LinkedList<T> data2) {
        if (data1.size() != data2.size()) return false;
        Iterator<T> it1 = data1.iterator();
        Iterator<T> it2 = data2.iterator();
        for (int i = 0; i < data1.size(); i++) {
            assert it1.hasNext();
            assert it2.hasNext();
            T o1 = it1.next();
            T o2 = it2.next();
            if (!Objects.equals(o1, o2)) {
                return false;
            }
        }

        assert !it1.hasNext();
        assert !it2.hasNext();

        return true;
    }

    @Test
    void test1() {
        LinkedDataRWD<String> ld = new LinkedDataRWD<>();
        ld.add("a").add("b").add("c");

        ReIterator<String> it = ld.iterator();
        Set<String> set = new HashSet<>();
        while (it.hasNext()) {
            set.add(it.next());
        }
        assert set.size() == 3;
        assert set.size() == ld.size();
        assert set.size() == it.reset();
    }

    @Test
    void test2() {
        LinkedDataRWD<String> ld = new LinkedDataRWD<>();
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
        LinkedDataRWD<String> ld = new LinkedDataRWD<>();
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

    @ParameterizedTest
    @ValueSource(strings = {"a", "b", "c", "d"})
    void remove1(String match) {
        LinkedDataRWD<String> ld = new LinkedDataRWD<>();
        ld.add("a").add("b").add("c").add("d");
        ReIterator<String> it = ld.iterator();

        while (it.hasNext()) {
            String s = it.next();
            if (s.equals(match)) {
                it.remove();
            }
        }
        assert it.reset() == 3;


    }

    @Test
    void remove2() {
        LinkedDataRWD<String> ld = new LinkedDataRWD<>();
        ld.add("a").add("b").add("c").add("d");

        LinkedList<String> ll = new LinkedList<>();
        ll.add("a");
        ll.add("b");
        ll.add("c");
        ll.add("d");


        Iterator<String> lli = ll.iterator();
        Iterator<String> ldi = ld.iterator();


        for (int i = 0; i < 4; i++) {
            String s1 = lli.next();
            String s2 = ldi.next();
            assert s1.equals(s2);

            ldi.remove();
            lli.remove();

            assert ll.size() == ld.size();
        }

        assert ld.first() == null;
        assert ld.last() == null;
        assert ld.size() == 0;

    }

    @Test
    void remove3() {
        LinkedDataRWD<String> ld = new LinkedDataRWD<>();
        ld.add("a").add("b").add("c").add("d");

        LinkedList<String> ll = new LinkedList<>();
        ll.add("a");
        ll.add("b");
        ll.add("c");
        ll.add("d");


        Iterator<String> lli = ll.iterator();
        ReIterator<String> ldi = ld.iterator();


        String s1, s2;
        for (int i = 0; i < 3; i++) {
            s1 = lli.next();
            s2 = ldi.next();
            assert s1.equals(s2);
        }

        s1 = lli.next();
        s2 = ldi.next();
        assert s1.equals(s2);

        // Removing last entry

        lli.remove();
        ldi.remove();
        assert ld.last().data.equals("c");
        assert ld.first().data.equals("a");

        Set<String> set1 = new HashSet<>(ll);
        Set<String> set2 = new HashSet<>();
        assert ldi.reset() == 3;
        ldi.forEachRemaining(set2::add);
        assert set1.equals(set2);
    }

    @Test
    void remove4() {
        LinkedDataRWD<Integer> ld = new LinkedDataRWD<>();
        LinkedList<Integer> ll = new LinkedList<>();

        // Fill collections
        for (int i = 0; i < 2048; i++) {
            ld.add(i);
            ll.add(i);
        }

        ReIterator<Integer> ldi = ld.iterator();

        // Randomly delete entries until both collections get empty
        while (!ll.isEmpty()) {
            int size = (int) ldi.reset();
            assert ll.size() == size;
            Iterator<Integer> lli = ll.iterator();
            Random r = new Random();

            for (int k = 0; k < size; k++) {
                int i1 = lli.next();
                int i2 = ldi.next();
                assert i1 == i2;
                boolean delete = r.nextInt(100) > 80;
                if (delete) {
                    lli.remove();
                    ldi.remove();
                }
            }

            assert sameData(ld, ll);
        }
    }

    @Test
    void remove5() {
        LinkedDataRWD<String> ld = new LinkedDataRWD<>();
        ld.add("a");

        LinkedList<String> ll = new LinkedList<>();
        ll.add("a");


        Iterator<String> lli = ll.iterator();
        Iterator<String> ldi = ld.iterator();


        String s1 = lli.next();
        String s2 = ldi.next();
        assert s1.equals(s2);

        ldi.remove();
        lli.remove();

        assert ll.size() == ld.size();

        assert ld.first() == null;
        assert ld.last() == null;
        assert ld.size() == 0;

    }

    @Test
    void consume1() {
        LinkedDataRWD<Integer> main = new LinkedDataRWD<>();
        LinkedDataRWD<Integer> delta = new LinkedDataRWD<>();

        int count = 2048;
        // Fill collections
        Set<Integer> set1 = new HashSet<>();
        for (int i = 0; i < count; i++) {
            main.add(i);
            delta.add(i + 1_000_000);

            set1.add(i);
            set1.add(i + 1_000_000);
        }

        ReIterator<Integer> it = main.iterator();
        assert it.reset() == main.size();

        main.consume(delta);
        assert delta.first() == null;
        assert delta.last() == null;
        assert delta.size() == 0;

        assert main.size() == count * 2;
        assert it.reset() == count * 2;
        assert main.last().data == 1_000_000 + count - 1;


        Set<Integer> set2 = new HashSet<>();
        it.forEachRemaining(set2::add);
        assert set1.equals(set2);
    }
}
