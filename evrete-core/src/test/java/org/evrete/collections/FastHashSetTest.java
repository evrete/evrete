package org.evrete.collections;

import org.evrete.api.ReIterator;
import org.evrete.classes.TypeA;
import org.evrete.helper.IterableSet;
import org.evrete.helper.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("AssertWithSideEffects")
class FastHashSetTest {

    private static void fill(Collection<String> data, IterableSet<String> fastSet, IterableSet<String> hashSet) {
        for (String s : data) {
            boolean b1 = fastSet.add(s);
            boolean b2 = hashSet.add(s);
            assert fastSet.size() == hashSet.size() : "Fast: " + fastSet.size() + ", Hash: " + hashSet.size();
            assert b1 == b2 : "b1=" + b1 + ", b2=" + b2;
        }
    }

    private static void remove(Collection<String> data, IterableSet<String> fastSet, IterableSet<String> hashSet) {
        for (String s : data) {

            boolean c1 = fastSet.contains(s);
            boolean c2 = hashSet.contains(s);
            assert c1 == c2;

            boolean r1 = fastSet.remove(s);
            boolean r2 = hashSet.remove(s);
            assert r1 == r2;
            assert fastSet.size() == hashSet.size() : "Fast: " + fastSet.size() + ", Hash: " + hashSet.size();
        }
    }


    @Test
    void basic1() {
        IterableSet<TypeA> set1 = TestUtils.setOf(new LinearHashSet<>(128));

        TypeA a1 = new TypeA();
        TypeA a2 = new TypeA();
        TypeA a3 = new TypeA();
        TypeA a4 = new TypeA();

        assert set1.add(a1);
        assert !set1.add(a1);
        assert set1.add(a2);
        assert set1.add(a3);


        assert set1.contains(a1) && set1.contains(a2) && set1.contains(a3);
        assert !set1.contains(a4);

        assert set1.size() == 3;

        set1.clear();
        assert set1.size() == 0;


        assert set1.add(a1);
        assert !set1.add(a1);
        assert set1.add(a2);
        assert set1.add(a3);

        assert set1.size() == 3;

        Set<TypeA> javaSet = new HashSet<>();
        AtomicInteger counter = new AtomicInteger(0);
        set1.forEach(a -> {
            counter.incrementAndGet();
            javaSet.add(a);
        });

        assert counter.get() == 3;
        assert javaSet.containsAll(Arrays.asList(a1, a2, a3));

        set1.delete(javaSet::contains);
        assert set1.size() == 0;
    }

    @Test
    void basic2() {
        LinearHashSet<TypeA> set1 = new LinearHashSet<>(16);

        TypeA a1 = new TypeA("A1");
        TypeA a2 = new TypeA("A2");
        TypeA a3 = new TypeA("A3");

        set1.add(a1);
        set1.add(a1);
        set1.add(a2);
        set1.add(a3);
        set1.assertStructure();

        for (int i = 0; i < 640; i++) {
            set1.remove(a3);
            set1.assertStructure();
            set1.add(a3);
            set1.assertStructure();
        }

    }

    @Test
    void sizeTest() {
        LinearHashSet<String> set1 = new LinearHashSet<>(5);
        assert set1.dataSize() == 8;

        LinearHashSet<String> set2 = new LinearHashSet<>(0);
        assert set2.dataSize() == 2;

        LinearHashSet<String> set3 = new LinearHashSet<>(32);
        assert set3.dataSize() == 32;
    }

    @Test
    void remove1() {
        LinearHashSet<String> set = new LinearHashSet<>();

        assert set.add("a");
        assert set.size() == 1;
        set.assertStructure();
        assert set.remove("a");
        set.assertStructure();
        assert !set.remove("a");
        assert set.size() == 0;
        set.assertStructure();
    }

    @Test
    void remove2() {
        LinearHashSet<String> fastSet = new LinearHashSet<>(16);

        // Prepare collection
        int totalEntries = 1_000_000;
        Collection<String> data = new ArrayList<>(totalEntries);
        for (int i = 0; i < totalEntries; i++) {
            data.add(String.valueOf(i % 3569));
        }

        // Fill
        for (String s : data) {
            fastSet.add(s);
            fastSet.assertStructure();
        }

        // Validate fill
        Set<String> javaSet = new HashSet<>(data);
        assert javaSet.size() == fastSet.size();
        AtomicInteger i1 = new AtomicInteger(0);
        fastSet.forEach(s -> {
            assert javaSet.contains(s);
            i1.incrementAndGet();
        });

        fastSet.stream().forEach(s -> {
            assert javaSet.contains(s);
        });

        assert i1.get() == javaSet.size();

        // Validate clear
        for (String s : data) {
            assert fastSet.remove(s) == javaSet.remove(s);
            fastSet.assertStructure();
        }

        assert fastSet.size() == 0;

    }

    @Test
    void remove3() {
        IterableSet<String> fastSet = TestUtils.setOf(new LinearHashSet<>(2));
        IterableSet<String> hashSet = TestUtils.setOf(new HashSet<>(2));

        // Fill first
        int totalEntries = 4096;
        Collection<String> data = new ArrayList<>(totalEntries);
        Random r = new Random(System.nanoTime());
        int max = 4096;
        for (int i = 0; i < totalEntries; i++) {
            data.add(String.valueOf(r.nextInt(max)));
        }

        // Validate fill
        fill(data, fastSet, hashSet);
        // Validate remove
        remove(data, fastSet, hashSet);
        assert fastSet.size() == 0;
        assert hashSet.size() == 0;
        // Validate fill again
        fill(data, fastSet, hashSet);
        assert fastSet.size() == new HashSet<>(data).size();
    }

    @Test
    void iterator1() {
        LinearHashSet<String> fastSet = new LinearHashSet<>();
        HashSet<String> hashSet = new HashSet<>();


        ReIterator<String> it = fastSet.iterator();
        assert !it.hasNext();


        // Fill first
        int totalEntries = 4096;
        Random r = new Random(System.nanoTime());
        int max = 4096;
        for (int i = 0; i < totalEntries; i++) {
            String s = String.valueOf(r.nextInt(max));
            fastSet.add(s);
            hashSet.add(s);
        }

        assert fastSet.size() == hashSet.size();

        AtomicInteger counter = new AtomicInteger();

        it.reset();
        while (it.hasNext()) {
            String s = it.next();
            assert hashSet.contains(s);
            counter.incrementAndGet();
        }

        assert counter.get() == hashSet.size();

        counter.set(0);
        it.reset();
        while (it.hasNext()) {
            String s = it.next();
            assert hashSet.contains(s);
            counter.incrementAndGet();
        }
        assert counter.get() == hashSet.size();


    }

    @Test
    void iterator2() {
        LinearHashSet<Integer> fastSet = new LinearHashSet<>();

        for (int i = 0; i < 100; i++) {
            fastSet.add(i);
        }

        Iterator<Integer> it = fastSet.iterator();

        while (it.hasNext()) {
            int i = it.next();
            if (i % 2 == 0) {
                it.remove();
            }
        }

        assert fastSet.size == 50;

    }
}