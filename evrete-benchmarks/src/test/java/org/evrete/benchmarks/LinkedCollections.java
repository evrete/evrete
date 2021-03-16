package org.evrete.benchmarks;

import org.evrete.jmh.ListCollections;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;


class LinkedCollections {

    @Test
    void benchmark() throws RunnerException {
        TimeValue duration = TimeValue.milliseconds(1000L);
        int iterations = 10;
        Options opt = new OptionsBuilder()
                .include(ListCollections.class.getSimpleName())
                .warmupIterations(iterations)
                .warmupTime(duration)
                .measurementIterations(iterations)
                .measurementTime(duration)
                .build();

        new Runner(opt).run();
    }
}
