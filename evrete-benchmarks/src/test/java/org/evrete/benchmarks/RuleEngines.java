package org.evrete.benchmarks;

import org.evrete.benchmarks.jmh.Drools01;
import org.evrete.benchmarks.jmh.SalesModel1;
import org.evrete.benchmarks.jmh.SalesModel2;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

class RuleEngines {
    @Test
    void salesMode1() throws RunnerException {
        TimeValue duration = TimeValue.milliseconds(1000L);
        int iterations = 10;
        Options opt = new OptionsBuilder()
                .include(SalesModel1.class.getSimpleName())
                .jvmArgsPrepend("-Djava.util.logging.config.file=src/test/resources/logging.properties")
                .result("sales1.csv")
                .resultFormat(ResultFormatType.CSV)
                .warmupIterations(iterations)
                .warmupTime(duration)
                .measurementIterations(iterations)
                .measurementTime(duration)
                .build();

        new Runner(opt).run();
    }

    @Test
    void salesMode2() throws RunnerException {
        TimeValue duration = TimeValue.milliseconds(1000L);
        int iterations = 10;
        Options opt = new OptionsBuilder()
                .include(SalesModel2.class.getSimpleName())
                .jvmArgsPrepend("-Djava.util.logging.config.file=src/test/resources/logging.properties")
                .result("sales2.csv")
                .resultFormat(ResultFormatType.CSV)
                .warmupIterations(iterations)
                .warmupTime(duration)
                .measurementIterations(iterations)
                .measurementTime(duration)
                .build();

        new Runner(opt).run();
    }

    @Test
    void miscModel() throws RunnerException {
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
