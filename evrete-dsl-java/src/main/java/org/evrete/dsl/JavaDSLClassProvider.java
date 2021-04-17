package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleScope;
import org.evrete.util.compiler.BytesClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;

public class JavaDSLClassProvider extends AbstractJavaDSLProvider {

    private static Knowledge apply(Knowledge knowledge, byte[][] bytes) {
        ClassLoader ctxClassLoader = knowledge.getClassLoader();
        ProtectionDomain domain = knowledge.getService().getSecurity().getProtectionDomain(RuleScope.BOTH);
        BytesClassLoader loader = new BytesClassLoader(ctxClassLoader, domain);
        for (byte[] arr : bytes) {
            JavaClassRuleSet jr = processRuleSet(knowledge, loader.buildClass(arr));
        }
        return knowledge;
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_C;
    }

    @Override
    public Knowledge create(KnowledgeService service, InputStream... streams) throws IOException {
        if (streams == null || streams.length == 0) throw new IOException("Empty sources");
        byte[][] bytes = new byte[streams.length][];
        for (int i = 0; i < streams.length; i++) {
            bytes[i] = toByteArray(streams[i]);
        }

        Knowledge delegate = service.newKnowledge();
        return apply(delegate, bytes);
    }
}
