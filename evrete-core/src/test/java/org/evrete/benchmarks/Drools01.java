package org.evrete.benchmarks;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.helper.SessionWrapper;
import org.evrete.helper.TestUtils;
import org.evrete.util.MapOfList;
import org.kie.api.runtime.KieContainer;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.evrete.api.FactBuilder.fact;

@SuppressWarnings("MethodMayBeStatic")
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 1)
public class Drools01 extends DroolsBase {

    @Benchmark
    public void simple(BenchmarkState state) {
        SessionWrapper s = state.getSession();
        List<Object> list = state.getCurrent();
        for (Object o : list) {
            s.insert(o);
        }
        s.fire();
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        final MapOfList<String, Object> objectMaps = new MapOfList<>();

        @Param({ONE_TO_1, ONE_TO_2, ONE_TO_4, ONE_TO_8, ONE_TO_16, ONE_TO_32, ONE_TO_64, ONE_TO_128, ONE_TO_256, ONE_TO_512})
        String uniqueness;

        @Param
        Implementation implementation;

        @Param({"100", "100000"})
        int rhsLoad;

        private KnowledgeService service;
        private KieContainer dKnowledge;
        private SessionWrapper droolsSession;
        private SessionWrapper evreteSession;

        SessionWrapper getSession() {
            switch (implementation) {
                case Drools:
                    return droolsSession;
                case Evrete:
                    return evreteSession;
                default:
                    throw new IllegalStateException();
            }
        }

        @Setup(Level.Invocation)
        public void resetSessions() {
            droolsSession.retractAll();
            evreteSession.retractAll();
        }

        @Setup(Level.Iteration)
        public void initObjects() {


            objectMaps.clear();
            for (Map.Entry<String, Integer> entry : UNIQUENESS_MAPPING.entrySet()) {
                int sparsityIndex = entry.getValue();
                String uniquenessLabel = entry.getKey();

                for (int i = 0; i < objectCount; i++) {
                    int j = i % (objectCount / sparsityIndex);
                    TypeA $a = new TypeA();
                    $a.setI(j);
                    TypeB $b = new TypeB();
                    $b.setI(i);
                    // Using the long field to store the wait time
                    $a.setL(rhsLoad);
                    objectMaps.add(uniquenessLabel, $a);
                    objectMaps.add(uniquenessLabel, $b);

                }
            }
        }

        List<Object> getCurrent() {
            return objectMaps.get(uniqueness);
        }

        @Setup(Level.Trial)
        public void initKnowledge() {
            service = new KnowledgeService();
            Knowledge eKnowledge = service.newKnowledge();
            eKnowledge.newRule("sample01")
                    .forEach(
                            fact("$a", TypeA.class),
                            fact("$b", TypeB.class)
                    )
                    .where("$a.i == $b.i")
                    .execute(ctx -> {
                        TypeA a = ctx.get("$a");
                        a.waitNs(a.getL());
                    });


            // Drools
            dKnowledge = TestUtils.droolsKnowledge("src/test/drl/sample01.drl");

            droolsSession = SessionWrapper.of(dKnowledge.newKieSession());
            evreteSession = SessionWrapper.of(eKnowledge.createSession());

        }

        @TearDown(Level.Trial)
        public void destroyKnowledge() {
            service.shutdown();
            dKnowledge.dispose();
        }
    }
}
