package org.evrete.benchmarks;

import org.evrete.api.ReIterator;
import org.evrete.classes.TypeA;
import org.evrete.collections.LinearHashSet;
import org.evrete.helper.IterableCollection;
import org.evrete.helper.IterableSet;
import org.evrete.helper.TestUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, warmups = 1)
@SuppressWarnings({"MethodMayBeStatic", "unused"})
public class Collections {
    private static final AtomicLong counter = new AtomicLong();

    @Benchmark
    public void scan(BenchState state) {
        IterableSet<TypeA> scanCollection = state.scanData.get(state.set);
        scanCollection.forEach(typeA -> counter.incrementAndGet());
    }

    @Benchmark
    public void iterator(BenchState state) {
        ReIterator<TypeA> it = state.scanData.get(state.set).iterator();
        while (it.hasNext()) {
            TypeA a = it.next();
            Blackhole.consumeCPU(a.hashCode() % 2);
        }
    }

    @Benchmark
    public void contains(BenchState state) {
        IterableSet<TypeA> scanCollection = state.scanData.get(state.set);
        boolean b = false;
        for (TypeA a : state.objects) {
            b = b ^ scanCollection.contains(a);
        }
        Blackhole.consumeCPU(b ? 1 : 2);
    }

    @Benchmark
    public void insert(BenchState state) {
        IterableSet<TypeA> scanCollection = state.addData.get(state.set);
        for (TypeA a : state.objects) {
            scanCollection.add(a);
        }
    }

    public enum SetImplementation {
        FastSet,
        HashSet
    }

    @State(Scope.Thread)
    public static class BenchState {
        private static final int objectCount = (1 << 17) + (1 << 15);
        private static final int initialSize = objectCount >> 10;
        final EnumMap<SetImplementation, IterableSet<TypeA>> scanData = new EnumMap<>(SetImplementation.class);
        final EnumMap<SetImplementation, IterableSet<TypeA>> addData = new EnumMap<>(SetImplementation.class);
        final ArrayList<TypeA> objects = new ArrayList<>(objectCount);
        @SuppressWarnings("unused")
        @Param
        SetImplementation set;

        public BenchState() {
            scanData.put(SetImplementation.HashSet, TestUtils.setOf(new HashSet<>(initialSize)));
            scanData.put(SetImplementation.FastSet, TestUtils.setOf(new LinearHashSet<>(initialSize)));
            addData.put(SetImplementation.HashSet, TestUtils.setOf(new HashSet<>(initialSize)));
            addData.put(SetImplementation.FastSet, TestUtils.setOf(new LinearHashSet<>(initialSize)));
        }

        @Setup(Level.Iteration)
        public void initObjects() {
            scanData.values().forEach(IterableCollection::clear);
            objects.clear();
            Random r = new Random();
            for (int i = 0; i < objectCount; i++) {
                int v = r.nextInt();
                TypeA a = new TypeA();
                a.setI(v);
                scanData.values().forEach(o -> o.add(a));
                objects.add(a);
            }
        }

        @Setup(Level.Invocation)
        public void clearObjects() {
            addData.values().forEach(IterableCollection::clear);
        }
    }
}
