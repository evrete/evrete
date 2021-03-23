package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleScope;
import org.evrete.api.RuntimeContext;
import org.evrete.util.compiler.BytesClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;

public class JavaDSLClassProvider extends AbstractJavaDSLProvider {

    private static void apply(RuntimeContext<?> targetContext, byte[][] bytes) {
        ClassLoader ctxClassLoader = targetContext.getClassLoader();
        ProtectionDomain domain = targetContext.getService().getSecurity().getProtectionDomain(RuleScope.BOTH);
        BytesClassLoader loader = new BytesClassLoader(ctxClassLoader, domain);
        for (byte[] arr : bytes) {
            processRuleSet(targetContext, new JavaClassRuleSet(loader.buildClass(arr)));
        }
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_C;
    }

    @Override
    public Knowledge create(KnowledgeService service, URL... resources) throws IOException {
        Knowledge knowledge = service.newKnowledge();
        apply(knowledge, resources);
        return knowledge;
    }

    @Override
    public void apply(RuntimeContext<?> targetContext, InputStream... streams) throws IOException {
        byte[][] bytes = new byte[streams.length][];
        for (int i = 0; i < streams.length; i++) {
            bytes[i] = toByteArray(streams[i]);
        }
        apply(targetContext, bytes);
    }
}
