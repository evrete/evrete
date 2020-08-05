package org.evrete.spi.minimal;

import org.evrete.benchmarks.BenchmarkExpressions;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

class ExpressionsBenchmarkTest {

    @Test
    void benchmark() throws RunnerException {
        TimeValue duration = TimeValue.milliseconds(500L);
        int iterations = 8;
        Options opt = new OptionsBuilder()
                .include(BenchmarkExpressions.class.getSimpleName())
                //.jvmArgsPrepend("--add-opens=java.base/java.io=ALL-UNNAMED")
                .warmupIterations(iterations)
                .warmupTime(duration)
                .measurementIterations(iterations)
                .measurementTime(duration)
                .build();

        new Runner(opt).run();
    }


}