package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleScope;
import org.evrete.util.compiler.BytesClassLoader;
import org.evrete.util.compiler.CompilationException;
import org.evrete.util.compiler.SourceCompiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.ProtectionDomain;

import static org.evrete.dsl.Utils.LOGGER;

public class DSLSourceProvider extends AbstractDSLProvider {

    private static final String CHARSET_PROPERTY = "org.evrete.source-charset";
    private static final String CHARSET_DEFAULT = "UTF-8";

    private static Knowledge build(Knowledge knowledge, String[] sources) {
        ClassLoader ctxClassLoader = knowledge.getClassLoader();
        ProtectionDomain domain = knowledge.getService().getSecurity().getProtectionDomain(RuleScope.BOTH);
        BytesClassLoader loader = new BytesClassLoader(ctxClassLoader, domain);
        SourceCompiler compiler = new SourceCompiler();
        Knowledge current = knowledge;
        for (String source : sources) {
            try {
                Class<?> ruleSet = compiler.compile(source, loader);
                current = processRuleSet(current, ruleSet);
            } catch (CompilationException e) {
                LOGGER.warning("Source code: \n" + e.getSource());
                throw new IllegalStateException(e);
            }
        }
        return current;
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_S;
    }

    @Override
    public Knowledge create(KnowledgeService service, InputStream... streams) throws IOException {
        if (streams == null || streams.length == 0) throw new IOException("Empty resources");
        String charSet = service.getConfiguration().getProperty(CHARSET_PROPERTY, CHARSET_DEFAULT);
        Knowledge knowledge = service.newKnowledge();
        return build(knowledge, toSourceString(Charset.forName(charSet), streams));
    }

    @Override
    public Knowledge create(KnowledgeService service, Reader... readers) throws IOException {
        if (readers == null || readers.length == 0) throw new IOException("Empty resources");
        Knowledge knowledge = service.newKnowledge();
        return build(knowledge, toSourceString(readers));
    }
}
