package org.evrete.benchmarks;

import org.evrete.jmh.Drools01;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

class DroolsBenchmarks {
    @Test
    void benchMark() throws RunnerException {
        TimeValue duration = TimeValue.milliseconds(1000L);
        int iterations = 10;
        Options opt = new OptionsBuilder()
                .include(Drools01.class.getSimpleName())
                .jvmArgsPrepend("-Djava.util.logging.config.file=src/test/resources/logging.properties")
                .result("benchmark.csv")
                .resultFormat(ResultFormatType.CSV)
                .warmupIterations(iterations)
                .warmupTime(duration)
                .measurementIterations(iterations)
                .measurementTime(duration)
                .build();

        new Runner(opt).run();
    }
}
