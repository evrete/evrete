package org.evrete.benchmarks.jmh;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.Evaluator;
import org.evrete.api.IntToValue;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleBuilder;
import org.evrete.benchmarks.models.misc.TypeA;
import org.evrete.benchmarks.models.misc.TypeB;
import org.evrete.benchmarks.models.misc.TypeC;
import org.evrete.runtime.KnowledgeRuntime;
import org.evrete.runtime.compiler.CompilationException;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, warmups = 1)
@SuppressWarnings({"unused"})
public class Expressions {
    private static final int count = 100_000;

    @Benchmark
    public void compiled(BenchState state) {
        boolean b = false;
        for (int i = 0; i < count; i++) {
            b ^= state.evaluator.test(state.func);
        }
        Blackhole.consumeCPU(b ? 1 : 0);
    }

    @Benchmark
    public void javaNative(BenchState state) {
        boolean b = false;
        for (int i = 0; i < count; i++) {
            b ^= state.test();
        }
        Blackhole.consumeCPU(b ? 1 : 0);
    }


    @State(Scope.Thread)
    public static class BenchState {
        static final AtomicLong counter = new AtomicLong();
        Evaluator evaluator;
        IntToValue func;
        private KnowledgeService service;


        public BenchState() {
        }

        private static boolean testInner(int i1, int i2, int i3) {
            return i1 + i2 + i3 > 10000;
        }


        @Setup(Level.Trial)
        public void initAll() throws CompilationException {
            service = new KnowledgeService(new Configuration());
            KnowledgeRuntime knowledge = (KnowledgeRuntime) service.newKnowledge();
            RuleBuilder<Knowledge> rule = knowledge.newRule();
            rule.forEach().addFactDeclaration("$a", TypeA.class);
            rule.forEach().addFactDeclaration("$b", TypeB.class.getName());
            rule.forEach().addFactDeclaration("$c", TypeC.class.getName());
            evaluator = knowledge.compile("$a.i + $b.i + $c.i > 10_000", rule);

            Random random = new Random();
            Object[] vars = new Object[8192 * 256];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = random.nextInt();
            }

            func = field -> {
                long l = counter.incrementAndGet() % vars.length;
                return vars[(int) l];
            };

        }

        boolean test() {
            return testInner((int) func.apply(0), (int) func.apply(1), (int) func.apply(2));
        }

        @TearDown(Level.Trial)
        public void destroyKnowledge() {
            service.shutdown();
        }
    }
}
