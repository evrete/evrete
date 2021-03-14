package org.evrete.dsl;

import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.DSLKnowledgeProvider;
import org.evrete.util.compiler.BytesClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class JavaDSLClassProvider extends AbstractJavaDSLProvider implements DSLKnowledgeProvider {
    static final String NAME = "JAVA-CLASS";

    private static void apply(RuntimeContext<?> targetContext, byte[] bytes) {
        ClassLoader ctxClassLoader = targetContext.getClassLoader();
        BytesClassLoader loader = new BytesClassLoader(ctxClassLoader);
        processRuleSet(targetContext, new JavaClassRuleSet(loader.buildClass(bytes)));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void apply(RuntimeContext<?> targetContext, InputStream inputStream) throws IOException {
        apply(targetContext, toByteArray(inputStream));
    }

    @Override
    public void apply(RuntimeContext<?> targetContext, URL... resources) throws IOException {
        if (resources == null || resources.length == 0) return;
        for (URL url : resources) {
            URLConnection conn = url.openConnection();
            apply(targetContext, conn.getInputStream());
        }
    }
}
