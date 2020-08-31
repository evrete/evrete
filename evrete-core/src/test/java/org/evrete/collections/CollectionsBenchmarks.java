package org.evrete.collections;

import org.evrete.benchmarks.Collections;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;


class CollectionsBenchmarks {

    /*
        Benchmark                      (implementation)  Mode  Cnt  Score   Error  Units
        BenchmarkCollections.contains           HashSet  avgt   10  0.278 ± 0.006  ms/op
        BenchmarkCollections.contains           FastSet  avgt   10  0.243 ± 0.004  ms/op
        BenchmarkCollections.scan               HashSet  avgt   10  0.421 ± 0.011  ms/op
        BenchmarkCollections.scan               FastSet  avgt   10  0.131 ± 0.001  ms/op

        BenchmarkCollections.contains           HashSet  avgt   10  0.289 ± 0.008  ms/op
        BenchmarkCollections.contains           FastSet  avgt   10  0.285 ± 0.007  ms/op
        BenchmarkCollections.scan               HashSet  avgt   10  0.515 ± 0.022  ms/op
        BenchmarkCollections.scan               FastSet  avgt   10  0.167 ± 0.002  ms/op
        BenchmarkCollections.stream             HashSet  avgt   10  0.344 ± 0.007  ms/op
        BenchmarkCollections.stream             FastSet  avgt   10  0.252 ± 0.007  ms/op
     */

    @Test
    void benchmark() throws RunnerException {
        TimeValue duration = TimeValue.milliseconds(1000L);
        int iterations = 10;
        Options opt = new OptionsBuilder()
                .include(Collections.class.getSimpleName())
                //.jvmArgsPrepend("--add-opens=java.base/java.io=ALL-UNNAMED")
                .warmupIterations(iterations)
                .warmupTime(duration)
                .measurementIterations(iterations)
                .measurementTime(duration)
                .build();

        new Runner(opt).run();
    }
}
