package org.evrete.collections;

import org.evrete.api.ReIterator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

class JoinReIteratorTest {

    @Test
    void scan1() {
        Collection<String> colA = new ArrayList<>();
        colA.add("A0");
        colA.add("A1");
        colA.add("A2");
        Collection<String> colB = new ArrayList<>();
        colB.add("B0");
        colB.add("B1");
        colB.add("B2");
        Collection<String> colC = new ArrayList<>();
        colC.add("C0");
        colC.add("C1");
        colC.add("C2");

        ReIterator<String> ia = new CollectionReIterator<>(colA);
        ReIterator<String> ib = new CollectionReIterator<>(colB);
        ReIterator<String> ic = new CollectionReIterator<>(colC);

        ReIterator<String> joined = JoinReIterator.of(ia, ib, ic);

        assert joined.reset() == colA.size() + colB.size() + colC.size();


        for (int i = 0; i < 10; i++) {
            int counter = 0;
            while (joined.hasNext()) {
                joined.next();
                counter++;
            }
            long l = joined.reset();
            assert counter == l;
        }
    }

    @Test
    void scan2() {
        Collection<String> colA = new ArrayList<>();
        Collection<String> colB = new ArrayList<>();
        colB.add("B0");
        colB.add("B1");
        colB.add("B2");
        Collection<String> colC = new ArrayList<>();
        colC.add("C0");
        colC.add("C1");
        colC.add("C2");

        ReIterator<String> ia = new CollectionReIterator<>(colA);
        ReIterator<String> ib = new CollectionReIterator<>(colB);
        ReIterator<String> ic = new CollectionReIterator<>(colC);

        ReIterator<String> joined = JoinReIterator.of(ia, ib, ic);

        assert joined.reset() == colA.size() + colB.size() + colC.size();

        int counter = 0;
        while (joined.hasNext()) {
            joined.next();
            counter++;
        }

        assert counter == joined.reset();
    }

    @Test
    void scan3() {
        Collection<String> colA = new ArrayList<>();
        colA.add("A0");
        colA.add("A1");
        colA.add("A2");
        Collection<String> colB = new ArrayList<>();
        Collection<String> colC = new ArrayList<>();
        colC.add("C0");
        colC.add("C1");
        colC.add("C2");

        ReIterator<String> ia = new CollectionReIterator<>(colA);
        ReIterator<String> ib = new CollectionReIterator<>(colB);
        ReIterator<String> ic = new CollectionReIterator<>(colC);

        ReIterator<String> joined = JoinReIterator.of(ia, ib, ic);

        assert joined.reset() == colA.size() + colB.size() + colC.size();

        int counter = 0;
        while (joined.hasNext()) {
            joined.next();
            counter++;
        }

        assert counter == joined.reset();
    }

    @Test
    void scan4() {
        Collection<String> colA = new ArrayList<>();
        colA.add("A0");
        colA.add("A1");
        colA.add("A2");
        Collection<String> colB = new ArrayList<>();
        colB.add("B0");
        colB.add("B1");
        colB.add("B2");
        Collection<String> colC = new ArrayList<>();

        ReIterator<String> ia = new CollectionReIterator<>(colA);
        ReIterator<String> ib = new CollectionReIterator<>(colB);
        ReIterator<String> ic = new CollectionReIterator<>(colC);

        ReIterator<String> joined = JoinReIterator.of(ia, ib, ic);

        assert joined.reset() == colA.size() + colB.size() + colC.size();

        int counter = 0;
        while (joined.hasNext()) {
            joined.next();
            counter++;
        }

        assert counter == joined.reset();
    }

    @Test
    void scan5() {
        Collection<String> colA = new ArrayList<>();
        Collection<String> colB = new ArrayList<>();
        colB.add("B0");
        colB.add("B1");
        colB.add("B2");
        Collection<String> colC = new ArrayList<>();

        ReIterator<String> ia = new CollectionReIterator<>(colA);
        ReIterator<String> ib = new CollectionReIterator<>(colB);
        ReIterator<String> ic = new CollectionReIterator<>(colC);

        ReIterator<String> joined = JoinReIterator.of(ia, ib, ic);

        assert joined.reset() == colA.size() + colB.size() + colC.size();

        int counter = 0;
        while (joined.hasNext()) {
            joined.next();
            counter++;
        }

        assert counter == joined.reset();
    }

    @Test
    void scan6() {
        Collection<String> colA = new ArrayList<>();
        Collection<String> colB = new ArrayList<>();
        Collection<String> colC = new ArrayList<>();

        ReIterator<String> ia = new CollectionReIterator<>(colA);
        ReIterator<String> ib = new CollectionReIterator<>(colB);
        ReIterator<String> ic = new CollectionReIterator<>(colC);

        ReIterator<String> joined = JoinReIterator.of(ia, ib, ic);

        assert joined.reset() == colA.size() + colB.size() + colC.size();

        int counter = 0;
        while (joined.hasNext()) {
            joined.next();
            counter++;
        }

        assert counter == joined.reset();
    }

    @Test
    void remove1() {
        Collection<String> colA = new ArrayList<>();
        colA.add("A0");
        colA.add("A1");
        colA.add("A2");
        Collection<String> colB = new ArrayList<>();
        colB.add("B0");
        colB.add("B1");
        colB.add("B2");
        Collection<String> colC = new ArrayList<>();
        colC.add("C0");
        colC.add("C1");
        colC.add("C2");

        ReIterator<String> ia = new CollectionReIterator<>(colA);
        ReIterator<String> ib = new CollectionReIterator<>(colB);
        ReIterator<String> ic = new CollectionReIterator<>(colC);

        ReIterator<String> joined = JoinReIterator.of(ia, ib, ic);

        assert joined.reset() == colA.size() + colB.size() + colC.size();

        int counter = 0;
        while (joined.hasNext()) {
            if (joined.next().endsWith("0")) {
                joined.remove();
            } else {
                counter++;
            }
        }

        assert colA.size() == 2;
        assert colB.size() == 2;
        assert colC.size() == 2;
        assert counter == joined.reset();
    }

    @Test
    void remove2() {
        Collection<String> colA = new ArrayList<>();
        colA.add("A0");
        colA.add("A1");
        colA.add("A2");
        Collection<String> colB = new ArrayList<>();
        colB.add("B0");
        colB.add("B1");
        colB.add("B2");
        Collection<String> colC = new ArrayList<>();
        colC.add("C0");
        colC.add("C1");
        colC.add("C2");

        ReIterator<String> ia = new CollectionReIterator<>(colA);
        ReIterator<String> ib = new CollectionReIterator<>(colB);
        ReIterator<String> ic = new CollectionReIterator<>(colC);

        ReIterator<String> joined = JoinReIterator.of(ia, ib, ic);

        assert joined.reset() == colA.size() + colB.size() + colC.size();

        for (int i = 0; i < 10; i++) {
            joined.reset();
            while (joined.hasNext()) {
                if (joined.next().endsWith(String.valueOf(i))) {
                    joined.remove();
                }
            }
        }


        assert colA.isEmpty();
        assert colB.isEmpty();
        assert colC.isEmpty();
        assert joined.reset() == 0;
    }
}
