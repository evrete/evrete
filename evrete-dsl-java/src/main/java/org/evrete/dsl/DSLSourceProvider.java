package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.JavaSourceCompiler;
import org.evrete.api.Knowledge;
import org.evrete.api.TypeResolver;
import org.evrete.util.CompilationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.logging.Level;

import static org.evrete.dsl.Utils.LOGGER;

/**
 * The DSLClassProvider class provides the implementation of the DSLKnowledgeProvider
 * interface for 'JAVA-SOURCE' DSL knowledge.
 */
public class DSLSourceProvider extends AbstractDSLProvider {

    private static final String CHARSET_PROPERTY = "org.evrete.source-charset";
    private static final String CHARSET_DEFAULT = "UTF-8";

    /**
     * Default public constructor
     */
    public DSLSourceProvider() {
    }

    private static Knowledge build(Knowledge knowledge, MethodHandles.Lookup lookup, String[] sources) {
        Knowledge current = knowledge;
        JavaSourceCompiler compiler = knowledge.getSourceCompiler();
        for (String source : sources) {
            try {
                Class<?> ruleSet = compiler.compile(source);
                current = processRuleSet(current, lookup, ruleSet);
            } catch (CompilationException e) {
                LOGGER.log(Level.WARNING,  e.getMessage(), e);
                throw new IllegalStateException(e);
            }
        }
        return current;
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_SOURCE;
    }

    @Override
    public Knowledge create(KnowledgeService service, TypeResolver typeResolver, InputStream... streams) throws IOException {
        if (streams == null || streams.length == 0) throw new IOException("Empty resources");
        String charSet = service.getConfiguration().getProperty(CHARSET_PROPERTY, CHARSET_DEFAULT);
        Knowledge knowledge = service.newKnowledge(typeResolver);
        MethodHandles.Lookup lookup = defaultLookup();
        return build(knowledge, lookup, toSourceString(Charset.forName(charSet), streams));
    }

    @Override
    public Knowledge create(KnowledgeService service, Reader... readers) throws IOException {
        if (readers == null || readers.length == 0) throw new IOException("Empty resources");
        Knowledge knowledge = service.newKnowledge();
        MethodHandles.Lookup lookup = defaultLookup();
        return build(knowledge, lookup, toSourceString(readers));
    }
}
