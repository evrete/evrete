package org.evrete.benchmarks.jmh;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;

import java.io.File;
import java.nio.file.Path;

public abstract class AbstractDrlBenchmark {

    static KieContainer droolsKnowledge(String file) {
        Path drlDir = Path.of(System.getProperty("evrete.drl.dir"));

        File drlFile = drlDir.resolve(file).toFile();

        KieServices ks = KieServices.get();
        KieRepository kr = ks.getRepository();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write(ResourceFactory.newFileResource(drlFile));
        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build Errors:\n" + kb.getResults().toString());
        }

        return ks.newKieContainer(kr.getDefaultReleaseId());
    }

}
