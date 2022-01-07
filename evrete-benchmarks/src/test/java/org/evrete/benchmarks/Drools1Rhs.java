package org.evrete.benchmarks;

import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;

public class Drools1Rhs {
    private static final AtomicInteger counter = new AtomicInteger(0);

    @SuppressWarnings("unused")
    public static void out(Object... objects) {
        StringJoiner joiner = new StringJoiner(" ");
        for (Object o : objects) {
            joiner.add(o.toString());
        }
        counter.incrementAndGet();
    }

    static void reset() {
        counter.set(0);
    }

    static void assertCount(int count) {
        assert counter.get() == count : "Actual " + count + " vs expected " + counter.get();
    }
}
