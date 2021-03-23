package org.evrete.dsl;

import org.evrete.api.RuntimeContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

class CommonTestMethods {

    static void applyToRuntimeAsStream(RuntimeContext<?> ctx, Class<?> ruleClass) {
        try {
            String url = ruleClass.getName().replaceAll("\\.", "/") + ".class";
            InputStream is = ruleClass.getClassLoader().getResourceAsStream(url);
            ctx.appendDslRules(AbstractJavaDSLProvider.PROVIDER_JAVA_C, is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void applyToRuntimeAsURL(RuntimeContext<?> ctx, Class<?> ruleClass) {
        try {
            String url = ruleClass.getName().replaceAll("\\.", "/") + ".class";
            URL u = ruleClass.getClassLoader().getResource(url);
            ctx.appendDslRules(AbstractJavaDSLProvider.PROVIDER_JAVA_C, u);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void applyToRuntimeAsFile(RuntimeContext<?> ctx, String dsl, File... files) {
        assert files != null && files.length > 0;
        try {
            for (File f : files) {
                assert f.exists() : "File " + f.getAbsolutePath() + " does not exist";
                ctx.appendDslRules(dsl, new FileInputStream(f));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void applyToRuntimeAsURLs(RuntimeContext<?> runtime, String dsl, File... files) {
        assert files != null && files.length > 0;
        try {
            URL[] urls = new URL[files.length];
            for (int i = 0; i < urls.length; i++) {
                File f = files[i];
                assert f.exists();
                urls[i] = f.toURI().toURL();
            }
            runtime.appendDslRules(dsl, urls);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
