package org.evrete.benchmarks;

import org.evrete.benchmarks.jmh.HashCollections;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;


@SuppressWarnings("NewClassNamingConvention")
class HashCollectionsBenchmarks {

    @Test
    void benchmark() throws RunnerException {
        TimeValue duration = TimeValue.milliseconds(1000L);
        int iterations = 10;
        Options opt = new OptionsBuilder()
                .include(HashCollections.class.getSimpleName())
                //.jvmArgsPrepend("--add-opens=java.base/java.io=ALL-UNNAMED")
                .warmupIterations(iterations)
                .warmupTime(duration)
                .measurementIterations(iterations)
                .measurementTime(duration)
                .build();

        new Runner(opt).run();
    }
}
