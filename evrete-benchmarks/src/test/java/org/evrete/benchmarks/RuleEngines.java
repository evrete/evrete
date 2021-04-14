package org.evrete.benchmarks;

import org.evrete.benchmarks.jmh.ImageModel1;
import org.evrete.benchmarks.jmh.ImageModel2;
import org.evrete.benchmarks.jmh.SalesModel;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class RuleEngines {
    private static final int ITERATIONS = 10;
    private static final TimeValue DURATION = TimeValue.milliseconds(1000L);
    private static final String[] JVM_ARGS = new String[]{
            "-Xms8G",
            "-Xmx8G",
            "-Djava.util.logging.config.file=src/test/resources/logging.properties"
    };

    private static final long RHS_TIME_NANOS = 100L;
    private static final long LHS_TIME_NANOS = 1000L;

    public static void rhsLoad() {
        long start = System.nanoTime();
        long end;
        do {
            end = System.nanoTime();
        } while (start + RHS_TIME_NANOS >= end);
    }

    public static void lhsLoad() {
        long start = System.nanoTime();
        long end;
        do {
            end = System.nanoTime();
        } while (start + LHS_TIME_NANOS >= end);
    }

    @Test
    void salesModel() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SalesModel.class.getSimpleName())
                .jvmArgs(JVM_ARGS)
                .result("benchmarks-sales-model.csv")
                .resultFormat(ResultFormatType.CSV)
                .warmupIterations(ITERATIONS)
                .warmupTime(DURATION)
                .measurementIterations(ITERATIONS)
                .measurementTime(DURATION)
                .build();

        new Runner(opt).run();
    }

    @Test
    void imageModel1() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ImageModel1.class.getSimpleName())
                .jvmArgs(JVM_ARGS)
                .result("benchmarks-image-model1.csv")
                .resultFormat(ResultFormatType.CSV)
                .warmupIterations(ITERATIONS)
                .warmupTime(DURATION)
                .measurementIterations(ITERATIONS)
                .measurementTime(DURATION)
                .build();

        new Runner(opt).run();
    }

    @Test
    void imageModel2() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ImageModel2.class.getSimpleName())
                .jvmArgs(JVM_ARGS)
                .result("benchmarks-image-model2.csv")
                .resultFormat(ResultFormatType.CSV)
                .warmupIterations(ITERATIONS)
                .warmupTime(DURATION)
                .measurementIterations(ITERATIONS)
                .measurementTime(DURATION)
                .build();

        new Runner(opt).run();
    }
}
