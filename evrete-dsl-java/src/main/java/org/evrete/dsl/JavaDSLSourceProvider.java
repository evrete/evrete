package org.evrete.dsl;

import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.DSLKnowledgeProvider;
import org.evrete.util.compiler.BytesClassLoader;
import org.evrete.util.compiler.SourceCompiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public class JavaDSLSourceProvider extends AbstractJavaDSLProvider implements DSLKnowledgeProvider {
    static final Logger LOGGER = Logger.getLogger(JavaDSLSourceProvider.class.getName());
    static final String NAME = "JAVA-SOURCE";
    private static final String CHARSET_PROPERTY = "org.evrete.source-charset";
    private static final String CHARSET_DEFAULT = "UTF-8";

    private static void apply(RuntimeContext<?> targetContext, String source) {
        ClassLoader ctxClassLoader = targetContext.getClassLoader();
        BytesClassLoader loader = new BytesClassLoader(ctxClassLoader);
        SourceCompiler compiler = new SourceCompiler();
        Class<?> ruleSet = compiler.compile(source, loader);
        processRuleSet(targetContext, new JavaClassRuleSet(ruleSet));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void apply(RuntimeContext<?> targetContext, InputStream inputStream) throws IOException {
        String charSet = targetContext.getConfiguration().getProperty(CHARSET_PROPERTY, CHARSET_DEFAULT);
        apply(targetContext, toSourceString(Charset.forName(charSet), inputStream));
    }

    @Override
    public void apply(RuntimeContext<?> targetContext, Reader reader) throws IOException {
        apply(targetContext, toSourceString(reader));
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
