package org.evrete.benchmarks.jmh;

import org.evrete.KnowledgeService;
import org.evrete.api.IntToValue;
import org.evrete.api.Knowledge;
import org.evrete.benchmarks.helper.SessionWrapper;
import org.evrete.benchmarks.helper.TestUtils;
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
public class ImageModel1 {

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
        }

        private static boolean imagePredicate(IntToValue values) {
            return values.apply(0) == values.apply(1);
        }

        @Setup(Level.Invocation)
        public void initSessions() {
            droolsSession = SessionWrapper.of(dKnowledge.newKieSession());
            evreteSession = SessionWrapper.of(eKnowledge.newStatefulSession());
            Collections.shuffle(images);
        }

        @TearDown(Level.Trial)
        public void destroyKnowledge() {
            service.shutdown();
            dKnowledge.dispose();
        }

        @Setup(Level.Trial)
        public void initKnowledge() {
            service = new KnowledgeService();
            eKnowledge = service.newKnowledge();
            eKnowledge.newRule("images")
                    .forEach(
                            "$model", ImageProcessor.class,
                            "$img1", Image.class,
                            "$img2", Image.class
                    )
                    .where(BenchmarkState::imagePredicate, "$img1.label", "$img2.label")
                    .execute(ctx -> {
                        Image img1 = ctx.get("$img1");
                        Image img2 = ctx.get("$img2");
                        ImageProcessor model = ctx.get("$model");
                        model.compute(img1, img2);
                    });

            // Drools
            dKnowledge = TestUtils.droolsKnowledge("src/test/drl/image-model1.drl");
        }

    }
}
