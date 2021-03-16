package org.evrete.jmh;

import org.evrete.api.ReIterator;
import org.evrete.classes.TypeA;
import org.evrete.collections.LinkedDataRW;
import org.evrete.collections.LinkedDataRWD;
import org.evrete.helper.IterableCollection;
import org.evrete.helper.TestUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, warmups = 1)
@SuppressWarnings({"MethodMayBeStatic", "unused"})
public class ListCollections {
    private static final AtomicLong counter = new AtomicLong();

    @Benchmark
    public void iterator(BenchState state) {
        ReIterator<TypeA> it = state.scanData.get(state.collection).iterator();
        while (it.hasNext()) {
            TypeA a = it.next();
            Blackhole.consumeCPU(1 + (System.identityHashCode(a) >> 31));
        }
    }

    @Benchmark
    public void nested(BenchState state) {
        ReIterator<TypeA> it = state.reIteratorData.get(state.collection).iterator();
        for (int i = 0; i < 1024; i++) {
            it.reset();
            while (it.hasNext()) {
                TypeA a = it.next();
                Blackhole.consumeCPU(1 + (System.identityHashCode(a) >> 31));
            }
        }
    }


    @Benchmark
    public void insert(BenchState state) {
        IterableCollection<TypeA> scanCollection = state.addData.get(state.collection);
        for (TypeA a : state.objects) {
            scanCollection.add(a);
        }
    }

    public enum ListImplementation {
        LinkedDataRWD,
        LinkedDataRW,
        LinkedList
    }

    @State(Scope.Thread)
    public static class BenchState {
        private static final int objectCount = 1 << 16;
        final EnumMap<ListImplementation, IterableCollection<TypeA>> scanData = new EnumMap<>(ListImplementation.class);
        final EnumMap<ListImplementation, IterableCollection<TypeA>> addData = new EnumMap<>(ListImplementation.class);
        final EnumMap<ListImplementation, IterableCollection<TypeA>> reIteratorData = new EnumMap<>(ListImplementation.class);
        final ArrayList<TypeA> objects = new ArrayList<>(objectCount);
        @SuppressWarnings("unused")
        @Param
        ListImplementation collection;

        public BenchState() {
            scanData.put(ListImplementation.LinkedList, TestUtils.collectionOf(new LinkedList<>()));
            scanData.put(ListImplementation.LinkedDataRWD, TestUtils.collectionOf(new LinkedDataRWD<>()));
            scanData.put(ListImplementation.LinkedDataRW, TestUtils.collectionOf(new LinkedDataRW<>()));
            addData.put(ListImplementation.LinkedList, TestUtils.collectionOf(new LinkedList<>()));
            addData.put(ListImplementation.LinkedDataRWD, TestUtils.collectionOf(new LinkedDataRWD<>()));
            addData.put(ListImplementation.LinkedDataRW, TestUtils.collectionOf(new LinkedDataRW<>()));
            reIteratorData.put(ListImplementation.LinkedList, TestUtils.collectionOf(new LinkedList<>()));
            reIteratorData.put(ListImplementation.LinkedDataRWD, TestUtils.collectionOf(new LinkedDataRWD<>()));
            reIteratorData.put(ListImplementation.LinkedDataRW, TestUtils.collectionOf(new LinkedDataRW<>()));
        }

        @Setup(Level.Iteration)
        public void initObjects() {
            scanData.values().forEach(IterableCollection::clear);
            reIteratorData.values().forEach(IterableCollection::clear);
            objects.clear();
            Random r = new Random();
            for (int i = 0; i < objectCount; i++) {
                int v = r.nextInt();
                TypeA a = new TypeA();
                a.setI(v);
                scanData.values().forEach(o -> o.add(a));
                objects.add(a);
            }
            for (int i = 0; i < objectCount / 1024; i++) {
                int v = r.nextInt();
                TypeA a = new TypeA();
                a.setI(v);
                reIteratorData.values().forEach(o -> o.add(a));
            }
        }

        @Setup(Level.Invocation)
        public void clearObjects() {
            addData.values().forEach(IterableCollection::clear);
        }
    }
}
