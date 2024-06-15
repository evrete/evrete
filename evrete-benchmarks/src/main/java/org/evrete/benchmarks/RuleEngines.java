package org.evrete.benchmarks;

import org.evrete.benchmarks.jmh.ImageModel1;
import org.evrete.benchmarks.jmh.ImageModel2;
import org.evrete.benchmarks.jmh.SalesModel;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.nio.file.Path;

public class RuleEngines {
    private static final int ITERATIONS = 10;
    private static final TimeValue DURATION = TimeValue.milliseconds(1000L);

    private static final long RHS_TIME_NANOS = 100L;
    private static final long LHS_TIME_NANOS = 1000L;

    public static void main(String[] args) throws RunnerException {
        if(args.length == 0) {
            throw new IllegalArgumentException("Root path expected");
        } else {
            Path rootPath = Path.of(args[0]);
            Path logConfig = rootPath.resolve("logging.properties");
            String[] jvmArgs = new String[] {
                    "-Xms1G",
                    "-Xmx1G",
                    "-Djava.util.logging.config.file=" + logConfig.toAbsolutePath(),
                    "-Devrete.drl.dir=" + rootPath.resolve("drl")
            };
            salesModel(jvmArgs);
            imageModel1(jvmArgs);
            imageModel2(jvmArgs);
        }


    }

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

    static void salesModel(String[] jvmArgs) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SalesModel.class.getSimpleName())
                .jvmArgs(jvmArgs)
                .result("benchmarks-sales-model.csv")
                .resultFormat(ResultFormatType.CSV)
                .warmupIterations(ITERATIONS)
                .warmupTime(DURATION)
                .measurementIterations(ITERATIONS)
                .measurementTime(DURATION)
                .build();

        new Runner(opt).run();
    }

    static void imageModel1(String[] jvmArgs) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ImageModel1.class.getSimpleName())
                .jvmArgs(jvmArgs)
                .result("benchmarks-image-model1.csv")
                .resultFormat(ResultFormatType.CSV)
                .warmupIterations(ITERATIONS)
                .warmupTime(DURATION)
                .measurementIterations(ITERATIONS)
                .measurementTime(DURATION)
                .build();

        new Runner(opt).run();
    }

    static void imageModel2(String[] jvmArgs) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ImageModel2.class.getSimpleName())
                .jvmArgs(jvmArgs)
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
