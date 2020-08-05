package org.evrete.collections;

import org.evrete.util.CollectionUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

class UnsignedIntArrayTest {

    @Test
    void add() {
        UnsignedIntArray arr = new UnsignedIntArray(4);
        arr.addNew(1);
        arr.addNew(2);
        arr.addNew(3);
        assert arr.currentInsertIndex() == 3;
        arr.addNew(4);
        assert arr.currentInsertIndex() == 4;
    }

    @Test
    void forEach() {
        AtomicInteger counter = new AtomicInteger(0);

        UnsignedIntArray arr = new UnsignedIntArray(4);
        arr.forEachInt(i -> counter.incrementAndGet());
        assert counter.get() == 0;
        counter.set(0);

        arr.addNew(1);
        arr.addNew(2);
        arr.addNew(3);
        assert arr.currentInsertIndex() == 3;
        arr.forEachInt(i -> counter.incrementAndGet());
        assert counter.get() == arr.currentInsertIndex();
        counter.set(0);

        arr.addNew(4);
        assert arr.currentInsertIndex() == 4;
        arr.forEachInt(i -> counter.incrementAndGet());
        assert counter.get() == arr.currentInsertIndex();
    }

    @Test
    void delete1() {
        UnsignedIntArray arr1 = new UnsignedIntArray(10);
        ArrayList<Integer> arr2 = new ArrayList<>(10);

        int[] data = new int[10_000];
        Random r = new Random(System.nanoTime());
        for (int i = 0; i < data.length; i++) {
            data[i] = r.nextInt(64);
        }

        for (int i = 0; i < data.length; i++) {
            arr1.addNew(data[i]);
            arr2.add(data[i]);
            if (i % 10 == 0) {
                assert arr1.currentInsertIndex() == arr2.size();
                assert arr2.size() == i + 1;
            }
        }
        assert arr1.currentInsertIndex() == arr2.size();

        // Deleting
        AtomicInteger dd = new AtomicInteger(0);
        for (int d = 0; d < 64; d++) {
            dd.set(d);
            boolean b1 = arr1.delete(i -> i == dd.get());
            boolean b2 = CollectionUtils.deleteFrom(arr2, i -> i == dd.get());
            assert b1 == b2;
            assert arr1.currentInsertIndex() == arr2.size();
        }
        assert arr1.currentInsertIndex() == 0;
        assert arr1.dataSize() < 10;
    }
}