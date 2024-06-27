package org.evrete.collections;

import org.evrete.helper.TestUtils;
import org.evrete.util.IndexedValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

class ForkingArrayTest {
    private static final ObjIntFunction<String, IndexedValue<String>> MAPPER = IndexedValue::new;

    @Test
    void sizeTests() {
        ForkingArray<IndexedValue<String>> a1 = new ForkingArray<>();
        ForkingArray<IndexedValue<String>> a2 = a1.newBranch();

        assert a1.getInitialArraySize() == a2.getInitialArraySize();
        assert a1.getInitialArraySize() == ForkingArray.DEFAULT_INITIAL_SIZE;

        ForkingArray<IndexedValue<String>> a3 = new ForkingArray<>(101);
        ForkingArray<IndexedValue<String>> a4 = a3.newBranch();

        assert a3.getInitialArraySize() == a4.getInitialArraySize();
        assert a3.getInitialArraySize() == 101;
    }

    @Test
    void appendOneLevelOnly() {
        ForkingArray<IndexedValue<String>> array = new ForkingArray<>(1);
        IndexedValue<String> i0 = array.append("a", MAPPER);
        assert i0.getIndex() == 0;
        assert i0.getValue().equals("a");
        IndexedValue<String> i1 = array.append("a", MAPPER);
        assert i1.getIndex() == 1;
        assert i1.getValue().equals("a");
        IndexedValue<String> i2 = array.append("a", MAPPER);
        assert i2.getIndex() == 2;
        assert i2.getValue().equals("a");

        assert array.get(0) == i0;
        assert array.get(1) == i1;
        assert array.get(2) == i2;
    }

    @Test
    void appendMultiLevels() {
        ForkingArray<IndexedValue<String>> level0 = new ForkingArray<>(1);
        IndexedValue<String> i0 = level0.append("a", MAPPER);
        assert i0.getIndex() == 0;
        assert i0.getValue().equals("a");
        IndexedValue<String> i1 = level0.append("a", MAPPER);
        assert i1.getIndex() == 1;
        assert i1.getValue().equals("a");
        IndexedValue<String> i2 = level0.append("a", MAPPER);
        assert i2.getIndex() == 2;
        assert i2.getValue().equals("a");

        assert level0.get(0) == i0;
        assert level0.get(1) == i1;
        assert level0.get(2) == i2;

        ForkingArray<IndexedValue<String>> level1 = level0.newBranch();
        assert level1.getDataOffset() == 3;

        IndexedValue<String> i3 = level1.append("a", MAPPER);
        assert i3.getIndex() == 3;
        IndexedValue<String> i4 = level1.append("a", MAPPER);
        assert i4.getIndex() == 4;
        IndexedValue<String> i5 = level1.append("a", MAPPER);
        assert i5.getIndex() == 5;

        assert level1.get(0) == level0.get(0);
        assert level1.get(1) == level0.get(1);
        assert level1.get(2) == level0.get(2);
        assert level1.get(3) == i3;
        assert level1.get(4) == i4;
        assert level1.get(5) == i5;

        // Requesting from a wrong level
        assert level0.get(3) == null;

        // Creating new levels should not change the results
        ForkingArray<IndexedValue<String>> level2 = level1.newBranch();
        assert level2.get(0) == level0.get(0);
        assert level2.get(1) == level0.get(1);
        assert level2.get(2) == level0.get(2);
        assert level2.get(3) == i3;
        assert level2.get(4) == i4;
        assert level2.get(5) == i5;


        ForkingArray<IndexedValue<String>> level3 = level2.newBranch();
        assert level3.get(0) == level0.get(0);
        assert level3.get(1) == level0.get(1);
        assert level3.get(2) == level0.get(2);
        assert level3.get(3) == i3;
        assert level3.get(4) == i4;
        assert level3.get(5) == i5;


        IndexedValue<String> i6 = level3.append("a", MAPPER);
        assert i6.getIndex() == 6;

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void singleBranchStreams(boolean parallel) {
        ForkingArray<IndexedValue<String>> array = new ForkingArray<>(16);

        Set<String> ref = new HashSet<>();
        for (int i = 0; i < 301; i++) {
            String s = "element" + i;
            array.append(s, MAPPER);
            ref.add(s);
        }

        final Random random = new Random();

        Set<String> data = Collections.synchronizedSet(new HashSet<>());
        array.stream(parallel).map(IndexedValue::getValue).forEach(s -> {
            TestUtils.sleep(random.nextInt(5));
            data.add(s);
        });

        assert data.equals(ref);

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void multiBranchStreams(boolean parallel) {
        ForkingArray<IndexedValue<String>> array = new ForkingArray<>(8);

        Set<String> ref = new HashSet<>();
        final Random random = new Random();
        for (int i = 0; i < 301; i++) {
            if (random.nextBoolean()) {
                array = array.newBranch();
            }
            String s = "element" + i;
            array.append(s, MAPPER);
            ref.add(s);
        }


        Set<String> data = Collections.synchronizedSet(new HashSet<>());
        array.stream(parallel).map(IndexedValue::getValue).forEach(s -> {
            TestUtils.sleep(random.nextInt(5));
            data.add(s);
        });
        assert data.equals(ref);
    }

    @Test
    void multiBranchIndependence() {
        ForkingArray<IndexedValue<String>> root = new ForkingArray<>(1);

        root.append("a", MAPPER);
        root.append("b", MAPPER);
        root.append("c", MAPPER);

        ForkingArray<IndexedValue<String>> branch1 = root.newBranch();
        branch1.append("d1", MAPPER);
        branch1.append("e1", MAPPER);
        branch1.append("f1", MAPPER);

        ForkingArray<IndexedValue<String>> branch2 = root.newBranch();
        branch2.append("d2", MAPPER);
        branch2.append("e2", MAPPER);
        branch2.append("f2", MAPPER);

        // First branch
        assert branch1.get(0).getIndex() == 0;
        assert branch1.get(0).getValue().equals("a");
        assert branch1.get(1).getIndex() == 1;
        assert branch1.get(1).getValue().equals("b");
        assert branch1.get(2).getIndex() == 2;
        assert branch1.get(2).getValue().equals("c");
        assert branch1.get(3).getIndex() == 3;
        assert branch1.get(3).getValue().equals("d1");
        assert branch1.get(4).getIndex() == 4;
        assert branch1.get(4).getValue().equals("e1");
        assert branch1.get(5).getIndex() == 5;
        assert branch1.get(5).getValue().equals("f1");

        // Second branch
        assert branch2.get(0).getIndex() == 0;
        assert branch2.get(0).getValue().equals("a");
        assert branch2.get(1).getIndex() == 1;
        assert branch2.get(1).getValue().equals("b");
        assert branch2.get(2).getIndex() == 2;
        assert branch1.get(2).getValue().equals("c");
        assert branch2.get(3).getIndex() == 3;
        assert branch2.get(3).getValue().equals("d2");
        assert branch2.get(4).getIndex() == 4;
        assert branch2.get(4).getValue().equals("e2");
        assert branch2.get(5).getIndex() == 5;
        assert branch2.get(5).getValue().equals("f2");

        // Appending data to the root branch
        root.append("d", MAPPER);
        root.append("e", MAPPER);
        root.append("f", MAPPER);

        // Same assertions must hold
        assert branch1.get(0).getIndex() == 0;
        assert branch1.get(0).getValue().equals("a");
        assert branch1.get(1).getIndex() == 1;
        assert branch1.get(1).getValue().equals("b");
        assert branch1.get(2).getIndex() == 2;
        assert branch1.get(2).getValue().equals("c");
        assert branch1.get(3).getIndex() == 3;
        assert branch1.get(3).getValue().equals("d1");
        assert branch1.get(4).getIndex() == 4;
        assert branch1.get(4).getValue().equals("e1");
        assert branch1.get(5).getIndex() == 5;
        assert branch1.get(5).getValue().equals("f1");


        assert branch2.get(0).getIndex() == 0;
        assert branch2.get(0).getValue().equals("a");
        assert branch2.get(1).getIndex() == 1;
        assert branch2.get(1).getValue().equals("b");
        assert branch2.get(2).getIndex() == 2;
        assert branch1.get(2).getValue().equals("c");
        assert branch2.get(3).getIndex() == 3;
        assert branch2.get(3).getValue().equals("d2");
        assert branch2.get(4).getIndex() == 4;
        assert branch2.get(4).getValue().equals("e2");
        assert branch2.get(5).getIndex() == 5;
        assert branch2.get(5).getValue().equals("f2");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void multiBranchStreamIndependence(boolean parallel) {
        ForkingArray<IndexedValue<String>> array = new ForkingArray<>(8);

        Set<String> ref = new HashSet<>();
        final Random random = new Random();

        Collection<ForkingArray<IndexedValue<String>>> branched = new ArrayList<>();

        for (int i = 0; i < 301; i++) {
            if (random.nextBoolean()) {
                branched.add(array);
                array = array.newBranch();
            }
            String s = "element" + i;
            array.append(s, MAPPER);
            ref.add(s);
        }

        // Now append new entries to branched arrays
        for (ForkingArray<IndexedValue<String>> branch : branched) {
            branch.append("extra1", MAPPER);
            branch.append("extra2", MAPPER);
            branch.append("extra3", MAPPER);
        }

        // Despite the additions, our current array should produce the same results
        Set<String> data = Collections.synchronizedSet(new HashSet<>());
        array.stream(parallel).map(IndexedValue::getValue).forEach(s -> {
            TestUtils.sleep(random.nextInt(5));
            data.add(s);
        });
        assert data.equals(ref);
    }


    @Test
    void updateTest() {
        ForkingArray<IndexedValue<String>> array = new ForkingArray<>(8);

        Set<String> ref = new HashSet<>();
        String prefix = "prefix";
        final Random random = new Random();
        for (int i = 0; i < 501; i++) {
            if (random.nextBoolean()) {
                array = array.newBranch();
            }
            String s = "element" + i;
            array.append(s, MAPPER);
            ref.add(prefix + s);
        }

        array.update(val -> new IndexedValue<>(val.getIndex(), prefix + val.getValue()));

        Set<String> data = Collections.synchronizedSet(new HashSet<>());
        array.forEach(stringIndexedValue -> data.add(stringIndexedValue.getValue()));

        Assertions.assertEquals(ref, data);

    }

}
