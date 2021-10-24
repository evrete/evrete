package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleScope;
import org.evrete.api.TypeResolver;
import org.evrete.util.compiler.ServiceClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;

public class DSLClassProvider extends AbstractDSLProvider {

    private static Knowledge apply(Knowledge knowledge, byte[][] bytes) {
        ClassLoader ctxClassLoader = knowledge.getClassLoader();
        ProtectionDomain domain = knowledge.getService().getSecurity().getProtectionDomain(RuleScope.BOTH);
        ServiceClassLoader loader = new ServiceClassLoader(ctxClassLoader, domain);
        Knowledge current = knowledge;
        for (byte[] arr : bytes) {
            current = processRuleSet(current, loader.buildClass(arr));
        }
        return current;
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_C;
    }


    @Override
    public Knowledge create(KnowledgeService service, TypeResolver typeResolver, InputStream... streams) throws IOException {
        if (streams == null || streams.length == 0) throw new IOException("Empty sources");
        byte[][] bytes = new byte[streams.length][];
        for (int i = 0; i < streams.length; i++) {
            bytes[i] = toByteArray(streams[i]);
        }

        Knowledge delegate = service.newKnowledge(typeResolver);
        return apply(delegate, bytes);
    }
}
