package org.evrete.benchmarks;

import org.evrete.benchmarks.jmh.ImageModel;
import org.evrete.benchmarks.jmh.SalesModel;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

class RuleEngines {
    @Test
    void salesModel() throws RunnerException {
        TimeValue duration = TimeValue.milliseconds(1000L);
        int iterations = 10;
        Options opt = new OptionsBuilder()
                .include(SalesModel.class.getSimpleName())
                .jvmArgs("-Xms8G", "-Xmx8G", "-Djava.util.logging.config.file=src/test/resources/logging.properties")
                .result("benchmarks-sales-report.csv")
                .resultFormat(ResultFormatType.CSV)
                .warmupIterations(iterations)
                .warmupTime(duration)
                .measurementIterations(iterations)
                .measurementTime(duration)
                .build();

        new Runner(opt).run();
    }

    @Test
    void imageModel() throws RunnerException {
        TimeValue duration = TimeValue.milliseconds(1000L);
        int iterations = 10;
        Options opt = new OptionsBuilder()
                .include(ImageModel.class.getSimpleName())
                .jvmArgs("-Xms8G", "-Xmx8G", "-Djava.util.logging.config.file=src/test/resources/logging.properties")
                .result("benchmarks-ml-model.csv")
                .resultFormat(ResultFormatType.CSV)
                .warmupIterations(iterations)
                .warmupTime(duration)
                .measurementIterations(iterations)
                .measurementTime(duration)
                .build();

        new Runner(opt).run();
    }
}
