package org.evrete.benchmarks.jmh;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.benchmarks.helper.SessionWrapper;
import org.evrete.benchmarks.helper.TestUtils;
import org.evrete.benchmarks.models.ml.DataModel;
import org.evrete.benchmarks.models.ml.Image;
import org.kie.api.runtime.KieContainer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("MethodMayBeStatic")
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 1)
public class ImageModel {

    @Benchmark
    public void drools(BenchmarkState state) {
        SessionWrapper s = state.droolsSession;
        for (Image i : state.images) {
            s.insert(i);
        }
        s.insert(new DataModel());
        s.fire();
        s.close();
    }


    @Benchmark
    public void evrete(BenchmarkState state) {
        SessionWrapper s = state.evreteSession;
        for (Image i : state.images) {
            s.insert(i);
        }
        s.insert(new DataModel());
        s.fire();
        s.close();
    }

    @Benchmark
    public void baseline(BenchmarkState state) {
        DataModel model = new DataModel();
        for (Image i1 : state.images) {
            for(Image i2 : state.images) {
                if (i1.label.equals(i2.label)) {
                    model.compute(i1, i2);
                }
            }
        }
        int b = model.blackHoleData > 1000? 1:2;
        Blackhole.consumeCPU(b);
    }

    @SuppressWarnings("unused")
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        private static final int LABELS = 4;
        private final Random random = new Random();
        List<Image> images;
        @Param({"1", "2", "4", "8", "16", "32", "64", "128", "256", "512"})
        int scale;
        SessionWrapper droolsSession;
        SessionWrapper evreteSession;
        private KnowledgeService service;
        private KieContainer dKnowledge;
        private Knowledge eKnowledge;

        @Setup(Level.Iteration)
        public void initInvocationData() {
            images = new ArrayList<>();

            for (int i = 0; i < scale * LABELS; i++) {
                int label = i % LABELS;
                Image image = new Image("Image-Label-" + label);
                images.add(image);
            }
            Collections.shuffle(images);
        }

        @Setup(Level.Invocation)
        public void initSessions() {
            droolsSession = SessionWrapper.of(dKnowledge.newKieSession());
            evreteSession = SessionWrapper.of(eKnowledge.createSession());
        }

        @Setup(Level.Trial)
        public void initKnowledge() {
            service = new KnowledgeService();
            eKnowledge = service.newKnowledge();
            eKnowledge.newRule("sample01")
                    .forEach(
                            "$model", DataModel.class,
                            "$img1", Image.class,
                            "$img2", Image.class
                    )
                    .where("$img1.label.equals($img2.label)")
                    .execute(ctx -> {
                        Image img1 = ctx.get("$img1");
                        Image img2 = ctx.get("$img2");
                        DataModel model = ctx.get("$model");
                        model.compute(img1, img2);
                    });

            // Drools
            dKnowledge = TestUtils.droolsKnowledge("src/test/drl/images.drl");
        }

        @TearDown(Level.Trial)
        public void destroyKnowledge() {
            service.shutdown();
            dKnowledge.dispose();
        }
    }
}
