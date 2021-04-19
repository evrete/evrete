package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

class CommonTestMethods {

    static Knowledge applyToRuntimeAsStream(KnowledgeService service, Class<?> ruleClass) {
        try {
            String url = ruleClass.getName().replaceAll("\\.", "/") + ".class";
            InputStream is = ruleClass.getClassLoader().getResourceAsStream(url);
            return service.newKnowledge(AbstractDSLProvider.PROVIDER_JAVA_C, is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static Knowledge applyToRuntimeAsURL(KnowledgeService service, Class<?> ruleClass) {
        try {
            String url = ruleClass.getName().replaceAll("\\.", "/") + ".class";
            URL u = ruleClass.getClassLoader().getResource(url);
            return service.newKnowledge(AbstractDSLProvider.PROVIDER_JAVA_C, u);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static Knowledge applyToRuntimeAsFile(KnowledgeService service, String dsl, File... files) {
        assert files != null && files.length > 0;
        InputStream[] streams = new InputStream[files.length];
        try {
            int i = 0;
            for (File f : files) {
                assert f.exists() : "File " + f.getAbsolutePath() + " does not exist";
                streams[i++] = new FileInputStream(f);
            }
            return service.newKnowledge(dsl, streams);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static Knowledge applyToRuntimeAsURLs(KnowledgeService service, String dsl, File... files) {
        assert files != null && files.length > 0;
        try {
            URL[] urls = new URL[files.length];
            for (int i = 0; i < urls.length; i++) {
                File f = files[i];
                assert f.exists();
                urls[i] = f.toURI().toURL();
            }
            return service.newKnowledge(dsl, urls);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
