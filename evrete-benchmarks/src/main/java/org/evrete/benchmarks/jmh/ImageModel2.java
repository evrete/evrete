package org.evrete.benchmarks.jmh;

import org.evrete.KnowledgeService;
import org.evrete.api.IntToValue;
import org.evrete.api.Knowledge;
import org.evrete.benchmarks.helper.SessionWrapper;
import org.evrete.benchmarks.models.ml.Image;
import org.evrete.benchmarks.models.ml.ImageProcessor;
import org.kie.api.runtime.KieContainer;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 1)
public class ImageModel2 extends AbstractDrlBenchmark {

    @Benchmark
    public void drools(BenchmarkState state) {
        SessionWrapper s = state.droolsSession;
        for (Image i : state.images) {
            s.insert(i);
        }
        s.insert(new ImageProcessor());
        s.fire();
        s.close();
    }


    @Benchmark
    public void evrete(BenchmarkState state) {
        SessionWrapper s = state.evreteSession;
        for (Image i : state.images) {
            s.insert(i);
        }
        s.insert(new ImageProcessor());
        s.fire();
        s.close();
    }

    @SuppressWarnings("unused")
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        private static final int LABELS = 3;
        private final Random random = new Random();
        List<Image> images;
        @Param({"1", "2", "4", "8", "16", "32", "64", "128", "256", "512"})
        int scale;
        SessionWrapper droolsSession;
        SessionWrapper evreteSession;
        private KnowledgeService service;
        private KieContainer dKnowledge;
        private Knowledge eKnowledge;

        private static boolean imagePredicate(IntToValue values) {
            ImageProcessor p = values.get(0);
            String label1 = values.get(1);
            String label2 = values.get(2);
            return p.test(label1, label2);
        }

        @Setup(Level.Iteration)
        public void initInvocationData() {
            images = new ArrayList<>();

            for (int i = 0; i < scale * 2; i++) {
                int label = i % LABELS;
                Image image = new Image("Image label - " + label);
                images.add(image);
            }
        }

        @Setup(Level.Invocation)
        public void initSessions() {
            droolsSession = SessionWrapper.of(dKnowledge.newKieSession());
            evreteSession = SessionWrapper.of(eKnowledge.newStatefulSession());
            Collections.shuffle(images);
        }

        @Setup(Level.Trial)
        public void initKnowledge() {
            service = new KnowledgeService();
            eKnowledge = service.newKnowledge();
            eKnowledge
                    .builder()
                    .newRule("sample01")
                    .forEach(
                            "$model", ImageProcessor.class,
                            "$img1", Image.class,
                            "$img2", Image.class
                    )
                    .where(BenchmarkState::imagePredicate, "$model", "$img1.label", "$img2.label")
                    .execute(ctx -> {
                        Image img1 = ctx.get("$img1");
                        Image img2 = ctx.get("$img2");
                        ImageProcessor model = ctx.get("$model");
                        model.compute(img1, img2);
                    })
                    .build();

            // Drools
            dKnowledge = droolsKnowledge("image-model2.drl");
        }

        @TearDown(Level.Trial)
        public void destroyKnowledge() {
            service.shutdown();
            dKnowledge.dispose();
        }

    }
}
